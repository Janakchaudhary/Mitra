package com.mitra.learning.ui.books

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.data.repository.BookRepository
import com.mitra.learning.data.repository.ImportBookResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddBookUiState(
    val selectedUri: Uri? = null,
    val selectedFileName: String? = null,
    val title: String = "",
    val subject: String = "Gujarati",
    val importing: Boolean = false,
    val importedBookId: String? = null,
    val error: String? = null,
)

class AddBookViewModel(private val repository: BookRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddBookUiState())
    val state: StateFlow<AddBookUiState> = _state.asStateFlow()

    fun onPdfSelected(uri: Uri) = viewModelScope.launch {
        val name = repository.displayName(uri)
        val defaultTitle = name?.substringBeforeLast('.') ?: "Standard 2 book"
        _state.value = _state.value.copy(
            selectedUri = uri,
            selectedFileName = name,
            title = defaultTitle,
            error = null,
        )
    }

    fun updateTitle(value: String) { _state.value = _state.value.copy(title = value, error = null) }
    fun updateSubject(value: String) { _state.value = _state.value.copy(subject = value, error = null) }

    fun import() {
        val current = _state.value
        val uri = current.selectedUri
        if (uri == null) {
            _state.value = current.copy(error = "Choose a PDF first")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(importing = true, error = null)
            when (val result = repository.importBook(uri, current.title, current.subject)) {
                is ImportBookResult.Success -> _state.value = _state.value.copy(
                    importing = false,
                    importedBookId = result.book.id,
                )
                is ImportBookResult.Duplicate -> _state.value = _state.value.copy(
                    importing = false,
                    error = "This PDF is already added as ‘${result.existing.title}’.",
                )
                is ImportBookResult.Failure -> _state.value = _state.value.copy(
                    importing = false,
                    error = result.message,
                )
            }
        }
    }
}
