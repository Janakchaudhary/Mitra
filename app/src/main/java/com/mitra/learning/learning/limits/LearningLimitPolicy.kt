package com.mitra.learning.learning.limits

import com.mitra.learning.data.db.entity.SessionEntity
import com.mitra.learning.data.db.entity.SessionStatus
import com.mitra.learning.settings.LearningSettings
import java.time.Instant
import java.time.ZoneId


data class LearningLimitStatus(
    val canStart: Boolean,
    val usedTodaySeconds: Int,
    val remainingTodaySeconds: Int,
    val sessionLimitSeconds: Int,
) {
    val usedTodayMinutes: Int get() = (usedTodaySeconds + 59) / 60
    val remainingTodayMinutes: Int get() = (remainingTodaySeconds + 59) / 60
}

object LearningLimitPolicy {
    fun status(
        settings: LearningSettings,
        sessions: List<SessionEntity>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LearningLimitStatus {
        val clean = settings.normalized()
        val startOfDay = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val usedSeconds = sessions.asSequence()
            .filter { it.startedAt >= startOfDay && it.startedAt <= nowMillis }
            .sumOf { session ->
                when (session.status) {
                    SessionStatus.COMPLETED, SessionStatus.STOPPED -> session.durationSeconds.coerceAtLeast(0)
                    SessionStatus.ACTIVE -> {
                        val elapsed = ((nowMillis - session.startedAt) / 1000L).coerceAtLeast(0L).toInt()
                        minOf(elapsed, clean.sessionMinutes * 60)
                    }
                }
            }

        val dailySeconds = clean.dailyMinutes * 60
        val remaining = (dailySeconds - usedSeconds).coerceAtLeast(0)
        return LearningLimitStatus(
            canStart = remaining > 0,
            usedTodaySeconds = usedSeconds,
            remainingTodaySeconds = remaining,
            sessionLimitSeconds = minOf(clean.sessionMinutes * 60, remaining),
        )
    }
}
