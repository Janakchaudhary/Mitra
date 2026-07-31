package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mitra.learning.data.db.entity.MasteryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasteryDao {
    @Query("SELECT * FROM mastery")
    fun observeAll(): Flow<List<MasteryEntity>>

    @Query("SELECT * FROM mastery")
    suspend fun getAll(): List<MasteryEntity>

    @Query("SELECT * FROM mastery WHERE conceptId = :conceptId LIMIT 1")
    suspend fun findByConceptId(conceptId: String): MasteryEntity?

    @Upsert
    suspend fun upsert(entity: MasteryEntity)

    @Query("DELETE FROM mastery")
    suspend fun deleteAll()
}
