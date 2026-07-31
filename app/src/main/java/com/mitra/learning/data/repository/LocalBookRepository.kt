package com.mitra.learning.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.mitra.learning.books.pdf.PdfPageRenderer
import com.mitra.learning.data.db.dao.BookDao
import com.mitra.learning.data.db.entity.BookAnalysisStatus
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.files.FileHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LocalBookRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val pdfRenderer: PdfPageRenderer,
    private val knowledgeRepository: BookKnowledgeRepository,
) : BookRepository {

    override fun observeBooks(): Flow<List<BookEntity>> = bookDao.observeAll()

    override fun observeBook(id: String): Flow<BookEntity?> = bookDao.observeById(id)

    override suspend fun getBook(id: String): BookEntity? = bookDao.findById(id)

    override suspend fun displayName(uri: Uri): String? = withContext(Dispatchers.IO) {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }
    }

    override suspend fun importBook(
        source: Uri,
        title: String,
        subject: String,
        standard: Int,
        language: String,
    ): ImportBookResult = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return@withContext ImportBookResult.Failure("Book title is required")
        if (subject.isBlank()) return@withContext ImportBookResult.Failure("Subject is required")

        val id = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "books/$id")
        val pdfFile = File(directory, "source.pdf")

        try {
            directory.mkdirs()
            val input = context.contentResolver.openInputStream(source)
                ?: return@withContext ImportBookResult.Failure("Could not open selected PDF")
            input.use { from -> pdfFile.outputStream().use { to -> from.copyTo(to) } }

            val sha = FileHasher.sha256(pdfFile)
            val duplicate = bookDao.findBySha256(sha)
            if (duplicate != null) {
                directory.deleteRecursively()
                return@withContext ImportBookResult.Duplicate(duplicate)
            }

            val pageCount = pdfRenderer.pageCount(pdfFile.absolutePath)
            if (pageCount <= 0) {
                directory.deleteRecursively()
                return@withContext ImportBookResult.Failure("The PDF has no readable pages")
            }

            val coverFile = File(directory, "cover.jpg")
            runCatching {
                val bitmap = pdfRenderer.render(pdfFile.absolutePath, 0, 640)
                coverFile.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
                }
                bitmap.recycle()
            }

            val book = BookEntity(
                id = id,
                title = cleanTitle,
                subject = subject.trim(),
                standard = standard,
                language = language,
                localPdfPath = pdfFile.absolutePath,
                sha256 = sha,
                pageCount = pageCount,
                coverPath = coverFile.takeIf(File::exists)?.absolutePath,
                createdAt = System.currentTimeMillis(),
                analysisStatus = BookAnalysisStatus.NOT_ANALYZED,
            )
            bookDao.insert(book)
            ImportBookResult.Success(book)
        } catch (t: Throwable) {
            directory.deleteRecursively()
            ImportBookResult.Failure(t.message ?: "Book import failed")
        }
    }

    override suspend fun deleteBook(id: String) = withContext(Dispatchers.IO) {
        val book = bookDao.findById(id) ?: return@withContext
        knowledgeRepository.deleteAllForBook(id)
        bookDao.delete(book)
        File(context.filesDir, "books/${book.id}").deleteRecursively()
    }
}
