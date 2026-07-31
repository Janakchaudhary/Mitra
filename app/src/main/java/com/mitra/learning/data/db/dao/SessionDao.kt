package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mitra.learning.data.db.entity.SessionEntity

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM learning_sessions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SessionEntity?

    @Query("SELECT * FROM learning_sessions ORDER BY startedAt DESC")
    suspend fun getAll(): List<SessionEntity>
}
