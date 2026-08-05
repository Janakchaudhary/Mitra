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
        val cleanSubject = subject.trim()
        if (cleanTitle.isBlank()) return@withContext ImportBookResult.Failure("Book title is required")
        if (cleanSubject.isBlank()) return@withContext ImportBookResult.Failure("Subject is required")

        // Import into a temporary book directory first. After hashing and counting pages,
        // Mitra can attach the PDF to an earlier ChatGPT-prepared package safely.
        val temporaryId = UUID.randomUUID().toString()
        val temporaryDirectory = File(context.filesDir, "books/$temporaryId")
        val temporaryPdf = File(temporaryDirectory, "source.pdf")

        try {
            temporaryDirectory.mkdirs()
            val input = context.contentResolver.openInputStream(source)
                ?: return@withContext ImportBookResult.Failure("Could not open selected PDF")
            input.use { from -> temporaryPdf.outputStream().use { to -> from.copyTo(to) } }

            val sha = FileHasher.sha256(temporaryPdf)
            val pageCount = pdfRenderer.pageCount(temporaryPdf.absolutePath)
            if (pageCount <= 0) {
                temporaryDirectory.deleteRecursively()
                return@withContext ImportBookResult.Failure("The PDF has no readable pages")
            }

            val exactHashMatch = bookDao.findBySha256(sha)
            if (exactHashMatch?.localPdfPath?.isNotBlank() == true) {
                temporaryDirectory.deleteRecursively()
                return@withContext ImportBookResult.Duplicate(exactHashMatch)
            }

            val metadataMatch = if (exactHashMatch == null) {
                bookDao.getAll().filter { candidate ->
                    candidate.localPdfPath.isBlank() &&
                        candidate.title.trim().equals(cleanTitle, ignoreCase = true) &&
                        candidate.subject.trim().equals(cleanSubject, ignoreCase = true) &&
                        candidate.standard == standard &&
                        candidate.pageCount == pageCount
                }.singleOrNull()
            } else {
                null
            }
            val preparedPackageBook = exactHashMatch ?: metadataMatch

            if (preparedPackageBook != null) {
                val destinationDirectory = File(context.filesDir, "books/${preparedPackageBook.id}").apply { mkdirs() }
                val destinationPdf = File(destinationDirectory, "source.pdf")
                temporaryPdf.copyTo(destinationPdf, overwrite = true)
                val coverFile = renderCover(destinationPdf, destinationDirectory)
                val updated = preparedPackageBook.copy(
                    title = cleanTitle,
                    subject = cleanSubject,
                    standard = standard,
                    language = language,
                    localPdfPath = destinationPdf.absolutePath,
                    sha256 = sha,
                    pageCount = pageCount,
                    coverPath = coverFile?.absolutePath ?: preparedPackageBook.coverPath,
                    // Keep READY when the imported ChatGPT package already contains knowledge.
                    analysisStatus = preparedPackageBook.analysisStatus,
                )
                bookDao.update(updated)
                temporaryDirectory.deleteRecursively()
                return@withContext ImportBookResult.Success(updated)
            }

            val coverFile = renderCover(temporaryPdf, temporaryDirectory)
            val book = BookEntity(
                id = temporaryId,
                title = cleanTitle,
                subject = cleanSubject,
                standard = standard,
                language = language,
                localPdfPath = temporaryPdf.absolutePath,
                sha256 = sha,
                pageCount = pageCount,
                coverPath = coverFile?.absolutePath,
                createdAt = System.currentTimeMillis(),
                analysisStatus = BookAnalysisStatus.NOT_ANALYZED,
            )
            bookDao.insert(book)
            ImportBookResult.Success(book)
        } catch (t: Throwable) {
            temporaryDirectory.deleteRecursively()
            ImportBookResult.Failure(t.message ?: "Book import failed")
        }
    }

    private suspend fun renderCover(pdfFile: File, directory: File): File? {
        val coverFile = File(directory, "cover.jpg")
        return runCatching {
            val bitmap = pdfRenderer.render(pdfFile.absolutePath, 0, 640)
            try {
                coverFile.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
                }
            } finally {
                bitmap.recycle()
            }
            coverFile.takeIf(File::exists)
        }.getOrNull()
    }

    override suspend fun deleteBook(id: String) = withContext(Dispatchers.IO) {
        val book = bookDao.findById(id) ?: return@withContext
        knowledgeRepository.deleteAllForBook(id)
        bookDao.delete(book)
        File(context.filesDir, "books/${book.id}").deleteRecursively()
    }
}
