package com.mitra.learning.data.repository

import android.net.Uri
import com.mitra.learning.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

sealed interface ImportBookResult {
    data class Success(val book: BookEntity) : ImportBookResult
    data class Duplicate(val existing: BookEntity) : ImportBookResult
    data class Failure(val message: String) : ImportBookResult
}

interface BookRepository {
    fun observeBooks(): Flow<List<BookEntity>>
    fun observeBook(id: String): Flow<BookEntity?>
    suspend fun getBook(id: String): BookEntity?
    suspend fun displayName(uri: Uri): String?
    suspend fun importBook(
        source: Uri,
        title: String,
        subject: String,
        standard: Int = 2,
        language: String = "Gujarati",
    ): ImportBookResult
    suspend fun deleteBook(id: String)
}
