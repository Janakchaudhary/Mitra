package com.mitra.learning.data.repository

import com.mitra.learning.data.db.dao.AttemptDao
import com.mitra.learning.data.db.dao.ConceptDao
import com.mitra.learning.data.db.dao.MasteryDao
import com.mitra.learning.data.db.dao.SessionDao
import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.data.db.entity.SessionEntity
import com.mitra.learning.learning.curriculum.BuiltInCurriculum
import kotlinx.coroutines.flow.Flow

class LocalLearningRepository(
    private val conceptDao: ConceptDao,
    private val masteryDao: MasteryDao,
    private val sessionDao: SessionDao,
    private val attemptDao: AttemptDao,
) : LearningRepository {
    override fun observeConcepts(): Flow<List<ConceptEntity>> = conceptDao.observeAll()

    override fun observeMastery(): Flow<List<MasteryEntity>> = masteryDao.observeAll()

    override suspend fun getConcepts(): List<ConceptEntity> = conceptDao.getPracticeReady()

    override suspend fun getPrerequisites(): List<ConceptPrerequisiteEntity> = conceptDao.getPrerequisites()

    override suspend fun getMastery(): List<MasteryEntity> = masteryDao.getAll()

    override suspend fun getMastery(conceptId: String): MasteryEntity? = masteryDao.findByConceptId(conceptId)

    override suspend fun seedBuiltInCurriculumIfNeeded() {
        if (conceptDao.builtInCount() < BuiltInCurriculum.concepts.size) {
            conceptDao.insertAll(BuiltInCurriculum.concepts)
            conceptDao.insertPrerequisites(BuiltInCurriculum.prerequisites)
        }
    }

    override suspend fun upsertMastery(mastery: MasteryEntity) = masteryDao.upsert(mastery)

    override suspend fun insertSession(session: SessionEntity) = sessionDao.insert(session)

    override suspend fun updateSession(session: SessionEntity) = sessionDao.update(session)

    override suspend fun findSession(id: String): SessionEntity? = sessionDao.findById(id)

    override suspend fun insertAttempt(attempt: AttemptEntity) = attemptDao.insert(attempt)

    override suspend fun attemptsForSession(sessionId: String): List<AttemptEntity> = attemptDao.forSession(sessionId)

    override suspend fun recentQuestionFingerprints(limit: Int): List<String> =
        attemptDao.recentQuestionFingerprints(limit)

    override suspend fun recentAttempts(limit: Int): List<AttemptEntity> = attemptDao.recent(limit)
}
