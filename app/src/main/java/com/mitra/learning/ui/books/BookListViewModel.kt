package com.mitra.learning.ui.books

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.books.importing.PreparedBookImportResult
import com.mitra.learning.books.importing.PreparedBookImportService
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PreparedBookImportUiState(
    val importing: Boolean = false,
    val message: String? = null,
    val importedBookId: String? = null,
)

class BookListViewModel(
    repository: BookRepository,
    private val preparedBookImportService: PreparedBookImportService,
) : ViewModel() {
    val books: StateFlow<List<BookEntity>> = repository.observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importState = MutableStateFlow(PreparedBookImportUiState())
    val importState: StateFlow<PreparedBookImportUiState> = _importState.asStateFlow()

    fun importPreparedBook(uri: Uri) = viewModelScope.launch {
        if (_importState.value.importing) return@launch
        _importState.value = PreparedBookImportUiState(importing = true, message = "Importing prepared book…")
        _importState.value = when (val result = preparedBookImportService.import(uri)) {
            is PreparedBookImportResult.Success -> PreparedBookImportUiState(
                importing = false,
                message = buildString {
                    append("Prepared book imported: ${result.chapterCount} chapters")
                    append(" and ${result.questionCount} offline questions.")
                    if (result.attachedToExistingPdf) append(" It was attached to the matching PDF.")
                },
                importedBookId = result.book.id,
            )
            is PreparedBookImportResult.Failure -> PreparedBookImportUiState(
                importing = false,
                message = "Could not import prepared book: ${result.message}",
            )
        }
    }

    fun consumeImportedBook() {
        _importState.value = _importState.value.copy(importedBookId = null)
    }
}
