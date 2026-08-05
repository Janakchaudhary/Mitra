package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mitra.learning.data.db.entity.VocabularyEntity

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary WHERE normalizedWord = :normalizedWord ORDER BY sourcePage LIMIT :limit")
    suspend fun findExact(normalizedWord: String, limit: Int = 8): List<VocabularyEntity>

    @Query("SELECT * FROM vocabulary WHERE chapterId = :chapterId ORDER BY sourcePage, word")
    suspend fun forChapter(chapterId: String): List<VocabularyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VocabularyEntity>)

    @Query("DELETE FROM vocabulary WHERE chapterId = :chapterId")
    suspend fun deleteForChapter(chapterId: String)

    @Transaction
    suspend fun replaceForChapter(chapterId: String, items: List<VocabularyEntity>) {
        deleteForChapter(chapterId)
        if (items.isNotEmpty()) upsertAll(items)
    }

    @Query("DELETE FROM vocabulary WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}
