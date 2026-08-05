package com.mitra.learning.learning.engine

import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveSessionPlannerTest {
    @Test
    fun incorrectExactFactIsPrioritizedOverRecentlyCorrectFact() {
        val weak = question("weak", "7 ગુણ્યા 8 કેટલા?", ActivityType.TABLES)
        val strong = question("strong", "2 ગુણ્યા 2 કેટલા?", ActivityType.TABLES)
        val now = 1_000_000L
        val attempts = listOf(
            attempt("a1", weak.fingerprint, AttemptResult.INCORRECT, now - 50_000),
            attempt("a2", weak.fingerprint, AttemptResult.INCORRECT, now - 40_000),
            attempt("a3", strong.fingerprint, AttemptResult.CORRECT, now - 10_000),
            attempt("a4", strong.fingerprint, AttemptResult.CORRECT, now - 5_000),
        )

        val selected = AdaptiveSessionPlanner.select(
            candidates = listOf(strong, weak),
            recentAttempts = attempts,
            count = 2,
            recentFingerprints = setOf(strong.fingerprint),
            nowMillis = now,
        )

        assertEquals(weak.id, selected.first().id)
    }

    @Test
    fun plannerAvoidsThreeIdenticalActivityTypesWhenAlternativeExists() {
        val candidates = listOf(
            question("n1", "1", ActivityType.QUESTION),
            question("n2", "2", ActivityType.QUESTION),
            question("n3", "3", ActivityType.QUESTION),
            question("v1", "word", ActivityType.VOCABULARY),
        )
        val selected = AdaptiveSessionPlanner.select(candidates, emptyList(), 4, nowMillis = 0)
        assertTrue(selected.take(3).map { it.type }.toSet().size > 1)
    }

    private fun question(id: String, prompt: String, type: ActivityType) = LearningQuestion(
        id = id,
        promptGujarati = prompt,
        expectedText = id,
        acceptedAnswers = listOf(id),
        activityType = type.name,
        evaluationMode = EvaluationMode.SHORT_TEXT,
    )

    private fun attempt(id: String, fingerprint: String, result: AttemptResult, time: Long) = AttemptEntity(
        id = id,
        sessionId = "session",
        conceptId = "concept",
        activityType = ActivityType.QUESTION.name,
        result = result,
        hintCount = 0,
        createdAt = time,
        questionFingerprint = fingerprint,
    )
}
