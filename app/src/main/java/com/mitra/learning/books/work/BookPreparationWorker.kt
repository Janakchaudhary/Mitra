package com.mitra.learning.books.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.mitra.learning.MitraApplication
import com.mitra.learning.books.analysis.BookPreparationResult
import com.mitra.learning.data.db.entity.PreparationJobEntity

class BookPreparationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val container get() = (applicationContext as MitraApplication).container
    private val jobDao get() = container.database.preparationJobDao()

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val bookId = inputData.getString(KEY_BOOK_ID) ?: return Result.failure()
        val chapterId = inputData.getString(KEY_CHAPTER_ID) ?: return Result.failure()
        setForeground(createForegroundInfo("પાઠ તૈયાર થાય છે…", 5))
        update(jobId, bookId, chapterId, "RUNNING", 5, "PDF લખાણ અને ચિત્રો વાંચે છે…")
        return try {
            when (val result = container.bookPreparationService.prepareChapter(chapterId) { percent, stage ->
                update(jobId, bookId, chapterId, "RUNNING", percent, stage)
            }) {
                is BookPreparationResult.Success -> {
                    update(jobId, bookId, chapterId, "SUCCEEDED", 100, "પાઠ અને પ્રશ્નો તૈયાર છે.")
                    Result.success()
                }
                is BookPreparationResult.Failure -> {
                    update(jobId, bookId, chapterId, "FAILED", 100, "પાઠ તૈયાર થઈ શક્યો નહીં.", result.message)
                    Result.failure()
                }
            }
        } catch (error: Throwable) {
            if (isStopped) {
                update(jobId, bookId, chapterId, "CANCELLED", 100, "તૈયારી રોકાઈ.")
                Result.failure()
            } else {
                update(
                    jobId, bookId, chapterId, "FAILED", 100,
                    "પાઠ તૈયાર થઈ શક્યો નહીં.",
                    error.message ?: "Unknown preparation error",
                )
                Result.failure()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo("પાઠ તૈયાર થાય છે…", 0)

    private suspend fun update(
        jobId: String,
        bookId: String,
        chapterId: String,
        status: String,
        progress: Int,
        stage: String,
        error: String? = null,
    ) {
        val existing = jobDao.findById(jobId)
        jobDao.upsert(
            PreparationJobEntity(
                id = jobId,
                bookId = bookId,
                chapterId = chapterId,
                status = status,
                progressPercent = progress.coerceIn(0, 100),
                currentStageGujarati = stage,
                errorMessage = error,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        )
        setProgress(androidx.work.workDataOf("progress" to progress, "stage" to stage))
        if (status == "RUNNING") setForeground(createForegroundInfo(stage, progress))
    }

    private fun createForegroundInfo(text: String, progress: Int): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Book preparation",
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Mitra પુસ્તક તૈયાર કરે છે")
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(progress in 0..99)
            .setProgress(100, progress.coerceIn(0, 100), progress <= 0)
            .build()
        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_BOOK_ID = "book_id"
        const val KEY_CHAPTER_ID = "chapter_id"
        private const val CHANNEL_ID = "mitra_book_preparation"
        private const val NOTIFICATION_ID = 2301
    }
}
