package com.mitra.learning.data.repository

import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

interface LearningRepository {
    fun observeConcepts(): Flow<List<ConceptEntity>>
    fun observeMastery(): Flow<List<MasteryEntity>>

    suspend fun getConcepts(): List<ConceptEntity>
    suspend fun getPrerequisites(): List<ConceptPrerequisiteEntity>
    suspend fun getMastery(): List<MasteryEntity>
    suspend fun getMastery(conceptId: String): MasteryEntity?

    suspend fun seedBuiltInCurriculumIfNeeded()
    suspend fun upsertMastery(mastery: MasteryEntity)

    suspend fun insertSession(session: SessionEntity)
    suspend fun updateSession(session: SessionEntity)
    suspend fun findSession(id: String): SessionEntity?

    suspend fun insertAttempt(attempt: AttemptEntity)
    suspend fun attemptsForSession(sessionId: String): List<AttemptEntity>
    suspend fun recentQuestionFingerprints(limit: Int = 40): List<String>
}
