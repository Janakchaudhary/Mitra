package com.mitra.learning.ui.assignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.data.db.dao.BookDao
import com.mitra.learning.data.db.dao.ChapterDao
import com.mitra.learning.data.db.dao.ConceptDao
import com.mitra.learning.data.db.dao.PreparedQuestionDao
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.learning.assignment.ParentQuizPlan
import com.mitra.learning.learning.assignment.ParentQuizRepository
import com.mitra.learning.learning.assignment.ParentQuizService
import com.mitra.learning.learning.assignment.ParentQuizTopic
import com.mitra.learning.learning.offline.OfflineQuestionBank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ParentQuizBuilderUiState(
    val title: String = "આજની કસોટી",
    val topic: ParentQuizTopic = ParentQuizTopic.PREPARED_BOOK,
    val questionCount: Int = 20,
    val selectedSkillConceptId: String = "builtin-math-add-under-20",
    val books: List<BookEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val selectedBookId: String? = null,
    val selectedChapterId: String? = null,
    val questionCountByChapter: Map<String, Int> = emptyMap(),
    val creating: Boolean = false,
    val activePlan: ParentQuizPlan? = null,
    val message: String? = null,
)

class ParentQuizBuilderViewModel(
    private val service: ParentQuizService,
    private val repository: ParentQuizRepository,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val preparedQuestionDao: PreparedQuestionDao,
    private val conceptDao: ConceptDao? = null,
    private val questionBank: OfflineQuestionBank? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentQuizBuilderUiState())
    val state: StateFlow<ParentQuizBuilderUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            bookDao.observeAll().collectLatest { books ->
                refreshBookCatalog(books)
            }
        }
    }

    fun setTitle(value: String) { _state.value = _state.value.copy(title = value.take(60), message = null) }

    fun setTopic(value: ParentQuizTopic) {
        _state.value = _state.value.copy(topic = value, message = null)
        if (value == ParentQuizTopic.PREPARED_BOOK && _state.value.selectedBookId == null) {
            viewModelScope.launch { refreshBookCatalog(bookDao.getAll()) }
        }
    }

    fun setQuestionCount(value: Int) {
        _state.value = _state.value.copy(questionCount = value.coerceIn(5, 25), message = null)
    }

    fun setSkillConcept(value: String) {
        _state.value = _state.value.copy(selectedSkillConceptId = value, message = null)
    }

    fun setBook(bookId: String) = viewModelScope.launch {
        loadChapters(bookId, preferredChapterId = null)
    }

    fun setChapter(chapterId: String) {
        val chapter = _state.value.chapters.firstOrNull { it.id == chapterId } ?: return
        _state.value = _state.value.copy(
            selectedChapterId = chapter.id,
            title = "${chapter.titleGujarati} કસોટી",
            message = null,
        )
    }

    fun create() = viewModelScope.launch {
        if (_state.value.creating) return@launch
        val current = _state.value
        if (current.topic == ParentQuizTopic.PREPARED_BOOK && current.selectedChapterId == null) {
            _state.value = current.copy(message = "પુસ્તક અને તૈયાર પાઠ પસંદ કરો.")
            return@launch
        }
        if (current.topic == ParentQuizTopic.PREPARED_BOOK) {
            val available = current.selectedChapterId?.let { current.questionCountByChapter[it] } ?: 0
            if (available < current.questionCount) {
                _state.value = current.copy(
                    message = "આ પાઠમાં $available અલગ પ્રશ્ન છે. ${current.questionCount} ગુણ માટે પાઠ ફરી Prepare કરો અથવા વધુ પ્રશ્નોવાળી .mitrabook Import કરો.",
                )
                return@launch
            }
        }
        _state.value = current.copy(creating = true, message = "પસંદ કરેલા પાઠમાંથી પ્રશ્નો ગોઠવાય છે…")
        runCatching {
            service.create(
                title = current.title,
                topic = current.topic,
                count = current.questionCount,
                skillConceptId = current.selectedSkillConceptId,
                bookId = current.selectedBookId,
                chapterId = current.selectedChapterId,
            )
        }.onSuccess { plan ->
            _state.value = _state.value.copy(
                creating = false,
                activePlan = plan,
                message = "${plan.questions.size} ગુણની '${plan.chapterTitleGujarati ?: plan.skillTitleGujarati ?: plan.topic.titleGujarati}' કસોટી બાળકના Home પર તૈયાર છે.",
            )
        }.onFailure { error ->
            _state.value = _state.value.copy(
                creating = false,
                message = error.message ?: "કસોટી બની શકી નહીં.",
            )
        }
    }

    fun clear() = viewModelScope.launch {
        repository.clear()
        _state.value = _state.value.copy(activePlan = null, message = "કસોટી દૂર થઈ.")
    }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(activePlan = repository.load())
    }

    private suspend fun refreshBookCatalog(books: List<BookEntity>) {
        val readyBooks = books.filter { book ->
            chapterDao.forBook(book.id).any { it.analysisStatus == ChapterAnalysisStatus.READY }
        }
        val current = _state.value.selectedBookId?.takeIf { id -> readyBooks.any { it.id == id } }
        val selected = current ?: readyBooks.firstOrNull()?.id
        _state.value = _state.value.copy(books = readyBooks, selectedBookId = selected)
        if (selected != null) loadChapters(selected, _state.value.selectedChapterId)
        else _state.value = _state.value.copy(chapters = emptyList(), selectedChapterId = null, questionCountByChapter = emptyMap())
    }

    private suspend fun loadChapters(bookId: String, preferredChapterId: String?) {
        val chapters = chapterDao.forBook(bookId)
            .filter { it.analysisStatus == ChapterAnalysisStatus.READY }
        val selected = preferredChapterId?.takeIf { id -> chapters.any { it.id == id } }
            ?: chapters.firstOrNull()?.id
        val counts = chapters.associate { chapter ->
            val roomCount = preparedQuestionDao.countForChapter(chapter.id)
            val legacyCount = if (roomCount == 0) {
                conceptDao?.forChapter(chapter.id).orEmpty().sumOf { concept ->
                    questionBank?.count(concept.id) ?: 0
                }
            } else 0
            chapter.id to (roomCount + legacyCount)
        }
        val selectedChapter = chapters.firstOrNull { it.id == selected }
        _state.value = _state.value.copy(
            selectedBookId = bookId,
            chapters = chapters,
            selectedChapterId = selected,
            questionCountByChapter = counts,
            title = selectedChapter?.let { "${it.titleGujarati} કસોટી" } ?: _state.value.title,
            message = if (chapters.isEmpty()) "આ પુસ્તકમાં READY પાઠ નથી." else null,
        )
    }
}
