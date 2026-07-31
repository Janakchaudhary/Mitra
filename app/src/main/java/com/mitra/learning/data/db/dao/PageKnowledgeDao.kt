package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mitra.learning.data.db.entity.PageKnowledgeEntity

@Dao
interface PageKnowledgeDao {
    @Query("SELECT * FROM page_knowledge WHERE chapterId = :chapterId ORDER BY pageNumber")
    suspend fun forChapter(chapterId: String): List<PageKnowledgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PageKnowledgeEntity>)

    @Query("DELETE FROM page_knowledge WHERE chapterId = :chapterId")
    suspend fun deleteForChapter(chapterId: String)

    @Query("DELETE FROM page_knowledge WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}
