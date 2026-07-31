package com.mitra.learning.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.books.analysis.BookPreparationResult
import com.mitra.learning.books.analysis.BookPreparationService
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.repository.BookKnowledgeRepository
import com.mitra.learning.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val bookId: String,
    private val repository: BookRepository,
    knowledgeRepository: BookKnowledgeRepository,
    private val preparationService: BookPreparationService,
) : ViewModel() {
    val book: StateFlow<BookEntity?> = repository.observeBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val chapters: StateFlow<List<ChapterEntity>> = knowledgeRepository.observeChapters(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _preparingChapterId = MutableStateFlow<String?>(null)
    val preparingChapterId: StateFlow<String?> = _preparingChapterId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun prepareChapter(chapterId: String) = viewModelScope.launch {
        if (_preparingChapterId.value != null) return@launch
        _preparingChapterId.value = chapterId
        _message.value = null
        _message.value = when (val result = preparationService.prepareChapter(chapterId)) {
            is BookPreparationResult.Success -> "Chapter prepared. ${result.sourceLabel}"
            is BookPreparationResult.Failure -> "Could not prepare chapter: ${result.message}"
        }
        _preparingChapterId.value = null
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        repository.deleteBook(bookId)
        onDeleted()
    }
}
