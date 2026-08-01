package com.mitra.learning.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.books.analysis.BookPreparationResult
import com.mitra.learning.books.analysis.BookPreparationService
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.repository.BookKnowledgeRepository
import com.mitra.learning.data.repository.BookRepository
import com.mitra.learning.learning.offline.OfflineQuestionBank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val bookId: String,
    private val repository: BookRepository,
    private val knowledgeRepository: BookKnowledgeRepository,
    private val preparationService: BookPreparationService,
    private val questionBank: OfflineQuestionBank? = null,
) : ViewModel() {
    val book: StateFlow<BookEntity?> = repository.observeBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val chapters: StateFlow<List<ChapterEntity>> = knowledgeRepository.observeChapters(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _preparingChapterId = MutableStateFlow<String?>(null)
    val preparingChapterId: StateFlow<String?> = _preparingChapterId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _conceptsByChapter = MutableStateFlow<Map<String, List<ConceptEntity>>>(emptyMap())
    val conceptsByChapter: StateFlow<Map<String, List<ConceptEntity>>> = _conceptsByChapter.asStateFlow()

    private val _offlineQuestionCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val offlineQuestionCounts: StateFlow<Map<String, Int>> = _offlineQuestionCounts.asStateFlow()

    init {
        viewModelScope.launch {
            chapters.collect { items ->
                val concepts = items.associate { chapter ->
                    chapter.id to knowledgeRepository.conceptsForChapter(chapter.id)
                }
                _conceptsByChapter.value = concepts
                _offlineQuestionCounts.value = concepts.values.flatten().associate { concept ->
                    concept.id to (questionBank?.count(concept.id) ?: 0)
                }
            }
        }
    }

    fun prepareChapter(chapterId: String) = viewModelScope.launch {
        if (_preparingChapterId.value != null) return@launch
        _preparingChapterId.value = chapterId
        _message.value = null
        _message.value = when (val result = preparationService.prepareChapter(chapterId)) {
            is BookPreparationResult.Success -> "Chapter prepared. ${result.sourceLabel}"
            is BookPreparationResult.Failure -> "Could not prepare chapter: ${result.message}"
        }
        _preparingChapterId.value = null
        val chapter = chapters.value.firstOrNull { it.id == chapterId }
        if (chapter != null) {
            val refreshed = knowledgeRepository.conceptsForChapter(chapterId)
            _conceptsByChapter.value = _conceptsByChapter.value + (chapterId to refreshed)
            _offlineQuestionCounts.value = _offlineQuestionCounts.value + refreshed.associate { concept ->
                concept.id to (questionBank?.count(concept.id) ?: 0)
            }
        }
    }


    fun setConceptEnabled(chapterId: String, conceptId: String, enabled: Boolean) = viewModelScope.launch {
        knowledgeRepository.setConceptPracticeReady(conceptId, enabled)
        _conceptsByChapter.value = _conceptsByChapter.value +
            (chapterId to knowledgeRepository.conceptsForChapter(chapterId))
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        repository.deleteBook(bookId)
        onDeleted()
    }
}
