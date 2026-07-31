package com.mitra.learning.learning.limits

import com.mitra.learning.data.db.dao.SessionDao
import com.mitra.learning.settings.LearningSettingsRepository

class LearningLimitService(
    private val settingsRepository: LearningSettingsRepository,
    private val sessionDao: SessionDao,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun status(): LearningLimitStatus = LearningLimitPolicy.status(
        settings = settingsRepository.get(),
        sessions = sessionDao.getAll(),
        nowMillis = now(),
    )
}
