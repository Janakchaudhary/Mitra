package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mitra.learning.data.db.entity.AttemptEntity

@Dao
interface AttemptDao {
    @Insert
    suspend fun insert(attempt: AttemptEntity)

    @Query("SELECT * FROM attempts WHERE sessionId = :sessionId ORDER BY createdAt")
    suspend fun forSession(sessionId: String): List<AttemptEntity>

    @Query("SELECT * FROM attempts ORDER BY createdAt DESC")
    suspend fun getAll(): List<AttemptEntity>

    @Query("SELECT questionFingerprint FROM attempts WHERE questionFingerprint != '' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentQuestionFingerprints(limit: Int): List<String>

    @Query("SELECT * FROM attempts WHERE questionFingerprint != '' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<AttemptEntity>

    @Query("DELETE FROM attempts")
    suspend fun deleteAll()
}
