package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConceptDao {
    @Query("SELECT * FROM concepts ORDER BY sortOrder, titleGujarati")
    fun observeAll(): Flow<List<ConceptEntity>>

    @Query("SELECT * FROM concepts ORDER BY sortOrder, titleGujarati")
    suspend fun getAll(): List<ConceptEntity>

    @Query("SELECT * FROM concepts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ConceptEntity?

    @Query("SELECT COUNT(*) FROM concepts WHERE builtIn = 1")
    suspend fun builtInCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(concepts: List<ConceptEntity>)

    @Query("SELECT * FROM concept_prerequisites")
    suspend fun getPrerequisites(): List<ConceptPrerequisiteEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPrerequisites(items: List<ConceptPrerequisiteEntity>)
}
