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

class LocalProgressRepository(
    private val conceptDao: ConceptDao,
    private val masteryDao: MasteryDao,
    private val sessionDao: SessionDao,
    private val attemptDao: AttemptDao,
) : ProgressRepository {
    override suspend fun concepts(): List<ConceptEntity> = conceptDao.getAll()
    override suspend fun mastery(): List<MasteryEntity> = masteryDao.getAll()
    override suspend fun prerequisites(): List<ConceptPrerequisiteEntity> = conceptDao.getPrerequisites()
    override suspend fun sessions(): List<SessionEntity> = sessionDao.getAll()
    override suspend fun attempts(): List<AttemptEntity> = attemptDao.getAll()
}
