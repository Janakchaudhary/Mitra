package com.mitra.learning.data.repository

import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.data.db.entity.SessionEntity

interface ProgressRepository {
    suspend fun concepts(): List<ConceptEntity>
    suspend fun mastery(): List<MasteryEntity>
    suspend fun prerequisites(): List<ConceptPrerequisiteEntity>
    suspend fun sessions(): List<SessionEntity>
    suspend fun attempts(): List<AttemptEntity>
}
