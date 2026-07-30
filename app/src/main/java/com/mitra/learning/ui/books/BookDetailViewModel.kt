package com.mitra.learning.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.repository.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val bookId: String,
    private val repository: BookRepository,
) : ViewModel() {
    val book: StateFlow<BookEntity?> = repository.observeBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        repository.deleteBook(bookId)
        onDeleted()
    }
}
