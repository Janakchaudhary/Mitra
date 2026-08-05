package com.mitra.learning.books.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mitra.learning.data.db.dao.PreparationJobDao
import com.mitra.learning.data.db.entity.PreparationJobEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class BookPreparationCoordinator(
    context: Context,
    private val jobDao: PreparationJobDao,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun observeJobs(bookId: String): Flow<List<PreparationJobEntity>> = jobDao.observeForBook(bookId)

    suspend fun enqueueChapter(bookId: String, chapterId: String): String {
        val job = newJob(bookId, chapterId)
        workManager.cancelUniqueWork(uniqueChapterName(chapterId))
        jobDao.cancelActiveForChapter(chapterId, now())
        jobDao.deleteFinishedForChapter(chapterId)
        jobDao.upsert(job)
        workManager.enqueueUniqueWork(
            uniqueChapterName(chapterId),
            ExistingWorkPolicy.REPLACE,
            request(job.id, bookId, chapterId),
        )
        return job.id
    }

    suspend fun enqueueBook(bookId: String, chapterIds: List<String>): List<String> {
        require(chapterIds.isNotEmpty()) { "પહેલાં પાઠ ઉમેરો." }
        workManager.cancelUniqueWork("prepare-book-$bookId")
        val jobs = chapterIds.distinct().map { chapterId ->
            workManager.cancelUniqueWork(uniqueChapterName(chapterId))
            jobDao.cancelActiveForChapter(chapterId, now())
            jobDao.deleteFinishedForChapter(chapterId)
            newJob(bookId, chapterId).also { jobDao.upsert(it) }
        }
        val requests = jobs.map { request(it.id, it.bookId, it.chapterId) }
        var continuation = workManager.beginUniqueWork(
            "prepare-book-$bookId",
            ExistingWorkPolicy.REPLACE,
            requests.first(),
        )
        requests.drop(1).forEach { continuation = continuation.then(it) }
        continuation.enqueue()
        return jobs.map { it.id }
    }

    fun cancelBook(bookId: String) {
        workManager.cancelUniqueWork("prepare-book-$bookId")
    }

    private fun request(jobId: String, bookId: String, chapterId: String): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<BookPreparationWorker>()
            .setInputData(
                workDataOf(
                    BookPreparationWorker.KEY_JOB_ID to jobId,
                    BookPreparationWorker.KEY_BOOK_ID to bookId,
                    BookPreparationWorker.KEY_CHAPTER_ID to chapterId,
                )
            )
            .addTag("mitra-book-preparation")
            .addTag("book-$bookId")
            .addTag("chapter-$chapterId")
            .build()

    private fun newJob(bookId: String, chapterId: String): PreparationJobEntity {
        val timestamp = now()
        return PreparationJobEntity(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            chapterId = chapterId,
            status = "QUEUED",
            progressPercent = 0,
            currentStageGujarati = "તૈયારી કતારમાં છે…",
            errorMessage = null,
            createdAt = timestamp,
            updatedAt = timestamp,
        )
    }

    private fun uniqueChapterName(chapterId: String) = "prepare-chapter-$chapterId"
}
