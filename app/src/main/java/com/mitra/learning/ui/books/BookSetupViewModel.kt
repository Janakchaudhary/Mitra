package com.mitra.learning.ui.books

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.books.analysis.BookPreparationService
import com.mitra.learning.books.analysis.ChapterDraft
import com.mitra.learning.books.pdf.PdfPageRenderer
import com.mitra.learning.data.repository.BookKnowledgeRepository
import com.mitra.learning.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class BookSetupUiState(
    val title: String = "Prepare book",
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val bitmap: Bitmap? = null,
    val loadingPage: Boolean = true,
    val selectedTocPages: Set<Int> = emptySet(),
    val detecting: Boolean = false,
    val saving: Boolean = false,
    val drafts: List<ChapterDraft> = emptyList(),
    val sourceLabel: String? = null,
    val message: String? = null,
    val error: String? = null,
    val saved: Boolean = false,
)

class BookSetupViewModel(
    private val bookId: String,
    private val bookRepository: BookRepository,
    private val knowledgeRepository: BookKnowledgeRepository,
    private val renderer: PdfPageRenderer,
    private val preparationService: BookPreparationService,
) : ViewModel() {
    private val _state = MutableStateFlow(BookSetupUiState())
    val state: StateFlow<BookSetupUiState> = _state.asStateFlow()
    private var pdfPath: String? = null

    init {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId)
            if (book == null) {
                _state.value = _state.value.copy(loadingPage = false, error = "Book not found")
                return@launch
            }
            pdfPath = book.localPdfPath
            val existing = knowledgeRepository.chaptersForBook(bookId).map {
                ChapterDraft(
                    id = it.id,
                    chapterNumber = it.chapterNumber,
                    titleGujarati = it.titleGujarati,
                    titleEnglish = it.titleEnglish,
                    startPage = it.startPage,
                    endPage = it.endPage,
                )
            }
            _state.value = _state.value.copy(
                title = "Prepare ${book.title}",
                pageCount = book.pageCount,
                drafts = existing,
                sourceLabel = if (existing.isNotEmpty()) "Saved chapter structure" else null,
            )
            render(0)
        }
    }

    fun nextPage() = render((_state.value.pageIndex + 1).coerceAtMost((_state.value.pageCount - 1).coerceAtLeast(0)))
    fun previousPage() = render((_state.value.pageIndex - 1).coerceAtLeast(0))

    fun toggleCurrentTocPage() {
        val index = _state.value.pageIndex
        val next = _state.value.selectedTocPages.toMutableSet()
        if (!next.add(index)) next.remove(index)
        _state.value = _state.value.copy(selectedTocPages = next)
    }

    fun detectChapters() {
        val pages = _state.value.selectedTocPages.sorted()
        if (pages.isEmpty()) {
            _state.value = _state.value.copy(error = "Mark at least one contents/index page")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(detecting = true, error = null, message = null)
            preparationService.detectChapters(bookId, pages)
                .onSuccess { (drafts, source) ->
                    _state.value = _state.value.copy(
                        detecting = false,
                        drafts = drafts,
                        sourceLabel = source,
                        message = "Review every chapter title and page range before saving.",
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(detecting = false, error = error.message ?: "Chapter detection failed")
                }
        }
    }

    fun addDraft() {
        val pageCount = _state.value.pageCount.coerceAtLeast(1)
        val lastEnd = _state.value.drafts.maxOfOrNull { it.endPage } ?: 0
        val start = (lastEnd + 1).coerceIn(1, pageCount)
        val number = (_state.value.drafts.mapNotNull { it.chapterNumber }.maxOrNull() ?: 0) + 1
        val draft = ChapterDraft(
            id = UUID.randomUUID().toString(),
            chapterNumber = number,
            titleGujarati = "પાઠ $number",
            startPage = start,
            endPage = start,
        )
        _state.value = _state.value.copy(drafts = _state.value.drafts + draft, error = null)
    }

    fun removeDraft(id: String) {
        _state.value = _state.value.copy(drafts = _state.value.drafts.filterNot { it.id == id })
    }

    fun updateTitle(id: String, value: String) = updateDraft(id) { it.copy(titleGujarati = value) }

    fun updateStartPage(id: String, value: String) {
        value.toIntOrNull()?.let { page -> updateDraft(id) { it.copy(startPage = page) } }
    }

    fun updateEndPage(id: String, value: String) {
        value.toIntOrNull()?.let { page -> updateDraft(id) { it.copy(endPage = page) } }
    }

    fun save() {
        if (_state.value.drafts.isEmpty()) {
            _state.value = _state.value.copy(error = "Add at least one chapter")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, message = null)
            preparationService.saveChapters(bookId, _state.value.drafts)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false, saved = true, message = "Chapter structure saved")
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(saving = false, error = error.message ?: "Could not save chapters")
                }
        }
    }

    private fun updateDraft(id: String, transform: (ChapterDraft) -> ChapterDraft) {
        _state.value = _state.value.copy(
            drafts = _state.value.drafts.map { if (it.id == id) transform(it) else it },
            error = null,
        )
    }

    private fun render(index: Int) {
        val path = pdfPath ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingPage = true, error = null)
            runCatching { renderer.render(path, index, 900) }
                .onSuccess { bitmap ->
                    val old = _state.value.bitmap
                    _state.value = _state.value.copy(pageIndex = index, bitmap = bitmap, loadingPage = false)
                    if (old !== bitmap) old?.recycle()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(loadingPage = false, error = error.message ?: "Unable to render page")
                }
        }
    }

    override fun onCleared() {
        _state.value.bitmap?.recycle()
    }
}
