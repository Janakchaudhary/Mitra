package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mitra.learning.data.db.entity.PreparedQuestionEntity

@Dao
interface PreparedQuestionDao {
    @Query("SELECT * FROM prepared_questions WHERE chapterId = :chapterId AND qualityStatus = 'APPROVED' ORDER BY usedCount, lastUsedAt, id LIMIT :limit")
    suspend fun forChapter(chapterId: String, limit: Int = 100): List<PreparedQuestionEntity>

    @Query("SELECT * FROM prepared_questions WHERE conceptId = :conceptId AND qualityStatus = 'APPROVED' ORDER BY usedCount, lastUsedAt, id LIMIT :limit")
    suspend fun forConcept(conceptId: String, limit: Int = 80): List<PreparedQuestionEntity>

    @Query("SELECT COUNT(DISTINCT fingerprint) FROM prepared_questions WHERE chapterId = :chapterId AND qualityStatus = 'APPROVED'")
    suspend fun countForChapter(chapterId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PreparedQuestionEntity>)

    @Query("UPDATE prepared_questions SET usedCount = usedCount + 1, lastUsedAt = :usedAt WHERE id IN (:ids)")
    suspend fun markUsed(ids: List<String>, usedAt: Long)

    @Query("DELETE FROM prepared_questions WHERE chapterId = :chapterId")
    suspend fun deleteForChapter(chapterId: String)

    @Transaction
    suspend fun replaceForChapter(chapterId: String, items: List<PreparedQuestionEntity>) {
        deleteForChapter(chapterId)
        if (items.isNotEmpty()) upsertAll(items)
    }

    @Query("DELETE FROM prepared_questions WHERE conceptId = :conceptId")
    suspend fun deleteForConcept(conceptId: String)

    @Query("DELETE FROM prepared_questions WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}
