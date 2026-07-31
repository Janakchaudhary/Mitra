package com.mitra.learning.learning.limits

import com.mitra.learning.data.db.entity.SessionEntity
import com.mitra.learning.data.db.entity.SessionStatus
import com.mitra.learning.settings.LearningSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class LearningLimitPolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = ZonedDateTime.of(2026, 7, 31, 16, 0, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `daily allowance blocks new session when exhausted`() {
        val sessions = listOf(session(now - 60_000, 30 * 60))
        val status = LearningLimitPolicy.status(
            settings = LearningSettings(sessionMinutes = 20, dailyMinutes = 30),
            sessions = sessions,
            nowMillis = now,
            zoneId = zone,
        )

        assertFalse(status.canStart)
        assertEquals(0, status.remainingTodaySeconds)
    }

    @Test
    fun `session limit is capped by remaining daily allowance`() {
        val sessions = listOf(session(now - 60_000, 25 * 60))
        val status = LearningLimitPolicy.status(
            settings = LearningSettings(sessionMinutes = 20, dailyMinutes = 30),
            sessions = sessions,
            nowMillis = now,
            zoneId = zone,
        )

        assertTrue(status.canStart)
        assertEquals(5 * 60, status.sessionLimitSeconds)
    }

    @Test
    fun `previous day sessions do not count`() {
        val yesterday = now - 24 * 60 * 60 * 1000L
        val status = LearningLimitPolicy.status(
            settings = LearningSettings(sessionMinutes = 20, dailyMinutes = 30),
            sessions = listOf(session(yesterday, 30 * 60)),
            nowMillis = now,
            zoneId = zone,
        )

        assertTrue(status.canStart)
        assertEquals(30 * 60, status.remainingTodaySeconds)
    }

    @Test
    fun `active session time counts toward allowance`() {
        val active = SessionEntity(
            id = "active",
            startedAt = now - 10 * 60 * 1000L,
            endedAt = null,
            primaryConceptId = null,
            activityCount = 0,
            durationSeconds = 0,
            status = SessionStatus.ACTIVE,
        )
        val status = LearningLimitPolicy.status(
            settings = LearningSettings(sessionMinutes = 20, dailyMinutes = 30),
            sessions = listOf(active),
            nowMillis = now,
            zoneId = zone,
        )

        assertEquals(10 * 60, status.usedTodaySeconds)
        assertEquals(20 * 60, status.remainingTodaySeconds)
    }

    private fun session(startedAt: Long, duration: Int) = SessionEntity(
        id = startedAt.toString(),
        startedAt = startedAt,
        endedAt = startedAt + duration * 1000L,
        primaryConceptId = null,
        activityCount = 1,
        durationSeconds = duration,
        status = SessionStatus.COMPLETED,
    )
}
