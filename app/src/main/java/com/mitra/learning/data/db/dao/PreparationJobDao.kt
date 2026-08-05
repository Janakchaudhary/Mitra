package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mitra.learning.data.db.entity.PreparationJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreparationJobDao {
    @Query("SELECT * FROM preparation_jobs WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun observeForBook(bookId: String): Flow<List<PreparationJobEntity>>

    @Query("SELECT * FROM preparation_jobs WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PreparationJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: PreparationJobEntity)


    @Query("UPDATE preparation_jobs SET status = 'CANCELLED', currentStageGujarati = 'જૂની તૈયારી બદલીને નવી શરૂ થઈ.', updatedAt = :updatedAt WHERE chapterId = :chapterId AND status IN ('QUEUED', 'RUNNING')")
    suspend fun cancelActiveForChapter(chapterId: String, updatedAt: Long)

    @Query("DELETE FROM preparation_jobs WHERE chapterId = :chapterId AND status IN ('SUCCEEDED', 'FAILED', 'CANCELLED')")
    suspend fun deleteFinishedForChapter(chapterId: String)

    @Query("DELETE FROM preparation_jobs")
    suspend fun deleteAll()
}
