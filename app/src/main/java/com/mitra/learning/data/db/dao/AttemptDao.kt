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
}
