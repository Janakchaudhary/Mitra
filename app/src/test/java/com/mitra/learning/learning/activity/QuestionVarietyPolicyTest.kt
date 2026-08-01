package com.mitra.learning.learning.activity

import com.mitra.learning.learning.model.LearningQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QuestionVarietyPolicyTest {
    @Test
    fun removesSessionDuplicatesAndPrefersFreshQuestions() {
        val old = LearningQuestion(id = "old", promptGujarati = "૨૩ + ૪ કેટલા?", expectedAnswer = 27)
        val fresh = LearningQuestion(id = "fresh", promptGujarati = "૩૨ + ૫ કેટલા?", expectedAnswer = 37)
        val duplicate = old.copy(id = "duplicate")

        val selected = QuestionVarietyPolicy.select(
            generated = listOf(old, duplicate, fresh),
            targetCount = 2,
            recentFingerprints = setOf(old.fingerprint),
        )

        assertEquals(2, selected.size)
        assertEquals(fresh.fingerprint, selected.first().fingerprint)
        assertFalse(selected.map { it.fingerprint }.let { it.size != it.distinct().size })
    }
}
