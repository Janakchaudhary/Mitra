package com.mitra.learning.learning.engine

import com.mitra.learning.learning.activity.ActivityPlanPolicy
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.ArithmeticWork
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixedSkillCoveragePolicyTest {
    @Test
    fun mixedPlanKeepsCarryConceptDiversityAndMovementBreak() {
        val candidates = listOf(
            question("eng-1", "english", ActivityType.VOCABULARY),
            question("eng-2", "english", ActivityType.VOCABULARY),
            question("guj-1", "gujarati", ActivityType.SPELLING),
            question("guj-2", "gujarati", ActivityType.SPELLING),
            question("plain-add", "plain-add", ActivityType.WORD_PROBLEM),
            question("table", "table-3", ActivityType.TABLES),
            question("carry", "carry-add", ActivityType.QUESTION, regrouping = true),
        )
        val adaptive = candidates.take(6)

        val covered = MixedSkillCoveragePolicy.apply(adaptive, candidates, count = 6)
        val safe = ActivityPlanPolicy.apply(covered)

        assertEquals(6, safe.size)
        assertTrue(safe.any { it.arithmeticWork?.regrouping == true })
        assertTrue(safe.mapNotNull { it.conceptId }.distinct().size >= 4)
        assertTrue(safe.any { it.type == ActivityType.PHYSICAL_MISSION })
        assertEquals(safe.size, safe.map { it.fingerprint }.distinct().size)
    }

    private fun question(
        id: String,
        conceptId: String,
        type: ActivityType,
        regrouping: Boolean = false,
    ) = LearningQuestion(
        id = id,
        promptGujarati = id,
        expectedAnswer = 1,
        activityType = type.name,
        evaluationMode = EvaluationMode.NUMERIC,
        conceptId = conceptId,
        arithmeticWork = if (type == ActivityType.QUESTION || type == ActivityType.WORD_PROBLEM) {
            ArithmeticWork(top = 27, bottom = 18, operator = "+", regrouping = regrouping)
        } else null,
    )
}
