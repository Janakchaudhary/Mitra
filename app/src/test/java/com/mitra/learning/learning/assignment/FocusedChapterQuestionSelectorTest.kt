package com.mitra.learning.learning.assignment

import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusedChapterQuestionSelectorTest {
    @Test
    fun selectionIsUniqueAndUsesMoreThanOneQuestionType() {
        val questions = (1..30).map { index ->
            LearningQuestion(
                id = "q$index",
                promptGujarati = "પ્રશ્ન $index",
                activityType = if (index % 3 == 0) ActivityType.VOCABULARY.name else ActivityType.MULTIPLE_CHOICE.name,
                evaluationMode = EvaluationMode.SHORT_TEXT,
                expectedText = "જવાબ $index",
                sourcePage = 10 + (index % 5),
                conceptId = "c${index % 4}",
            )
        }

        val selected = FocusedChapterQuestionSelector.select(questions, 20)

        assertEquals(20, selected.size)
        assertEquals(20, selected.map { it.fingerprint }.distinct().size)
        assertTrue(selected.map { it.activityType }.distinct().size > 1)
        assertTrue(selected.mapNotNull { it.sourcePage }.distinct().size > 1)
    }
}
