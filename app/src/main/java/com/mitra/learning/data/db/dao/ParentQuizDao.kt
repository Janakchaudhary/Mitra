package com.mitra.learning.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mitra.learning.data.db.entity.ParentQuizPlanEntity
import com.mitra.learning.data.db.entity.ParentQuizQuestionEntity

@Dao
interface ParentQuizDao {
    @Query("SELECT * FROM parent_quiz_plans ORDER BY createdAt DESC LIMIT 1")
    suspend fun activePlan(): ParentQuizPlanEntity?

    @Query("SELECT * FROM parent_quiz_questions WHERE planId = :planId ORDER BY position")
    suspend fun questions(planId: String): List<ParentQuizQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: ParentQuizPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<ParentQuizQuestionEntity>)

    @Query("DELETE FROM parent_quiz_questions")
    suspend fun deleteQuestions()

    @Query("DELETE FROM parent_quiz_plans")
    suspend fun deletePlans()

    @Transaction
    suspend fun replace(plan: ParentQuizPlanEntity, questions: List<ParentQuizQuestionEntity>) {
        deleteQuestions()
        deletePlans()
        insertPlan(plan)
        insertQuestions(questions)
    }

    @Transaction
    suspend fun clear() {
        deleteQuestions()
        deletePlans()
    }
}
