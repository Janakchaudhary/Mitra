package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mitra.learning.data.db.entity.PageKnowledgeFtsEntity

@Dao
interface PageKnowledgeFtsDao {
    @Query("SELECT * FROM page_knowledge_fts WHERE page_knowledge_fts MATCH :query LIMIT :limit")
    suspend fun search(query: String, limit: Int = 80): List<PageKnowledgeFtsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PageKnowledgeFtsEntity>)

    @Query("DELETE FROM page_knowledge_fts WHERE chapterId = :chapterId")
    suspend fun deleteForChapter(chapterId: String)

    @Query("DELETE FROM page_knowledge_fts WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)
}
