package com.mitra.learning.ui.books

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.books.pdf.PdfPageRenderer
import com.mitra.learning.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PdfViewerUiState(
    val title: String = "Book",
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val bitmap: Bitmap? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class PdfViewerViewModel(
    private val bookId: String,
    private val repository: BookRepository,
    private val renderer: PdfPageRenderer,
) : ViewModel() {
    private val _state = MutableStateFlow(PdfViewerUiState())
    val state: StateFlow<PdfViewerUiState> = _state.asStateFlow()
    private var path: String? = null

    init {
        viewModelScope.launch {
            val book = repository.getBook(bookId)
            if (book == null) {
                _state.value = _state.value.copy(loading = false, error = "Book not found")
            } else {
                path = book.localPdfPath
                _state.value = _state.value.copy(title = book.title, pageCount = book.pageCount)
                render(0)
            }
        }
    }

    fun next() {
        val target = (_state.value.pageIndex + 1).coerceAtMost(_state.value.pageCount - 1)
        render(target)
    }

    fun previous() {
        render((_state.value.pageIndex - 1).coerceAtLeast(0))
    }

    private fun render(index: Int) {
        val file = path ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { renderer.render(file, index, 1200) }
                .onSuccess { bitmap ->
                    _state.value = _state.value.copy(
                        pageIndex = index,
                        bitmap = bitmap,
                        loading = false,
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(loading = false, error = error.message)
                }
        }
    }

    override fun onCleared() {
        _state.value.bitmap?.recycle()
    }
}
