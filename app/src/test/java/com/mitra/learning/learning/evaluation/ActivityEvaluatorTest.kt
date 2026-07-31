package com.mitra.learning.learning.evaluation

import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityEvaluatorTest {
    @Test
    fun numericAcceptsGujaratiDigits() {
        val activity = LearningQuestion(id = "n", promptGujarati = "?", expectedAnswer = 7)
        assertEquals(AttemptResult.CORRECT, ActivityEvaluator.evaluate(activity, "૭"))
    }

    @Test
    fun multipleChoiceAcceptsEquivalentAnswer() {
        val activity = LearningQuestion(
            id = "m",
            promptGujarati = "?",
            evaluationMode = EvaluationMode.MULTIPLE_CHOICE,
            expectedText = "હાથી",
            acceptedAnswers = listOf("હાથી", "એ હાથી છે"),
            optionsGujarati = listOf("હાથી", "સિંહ"),
        )
        assertEquals(AttemptResult.CORRECT, ActivityEvaluator.evaluate(activity, "હાથી"))
    }

    @Test
    fun keywordCanMatchInsideShortSentence() {
        val activity = LearningQuestion(
            id = "k",
            promptGujarati = "?",
            evaluationMode = EvaluationMode.KEYWORD,
            acceptedAnswers = listOf("જંગલ"),
        )
        assertEquals(AttemptResult.CORRECT, ActivityEvaluator.evaluate(activity, "હાથી જંગલમાં રહે છે"))
    }

    @Test
    fun spellingComparisonIsCaseAndPunctuationTolerantButNotLetterTolerant() {
        val activity = LearningQuestion(
            id = "spell",
            promptGujarati = "Spell it",
            evaluationMode = EvaluationMode.SHORT_TEXT,
            expectedText = "Book",
        )
        assertEquals(AttemptResult.CORRECT, ActivityEvaluator.evaluate(activity, "book!"))
        assertEquals(AttemptResult.INCORRECT, ActivityEvaluator.evaluate(activity, "bok"))
    }

    @Test
    fun participationIsNeverScored() {
        val activity = LearningQuestion(
            id = "p",
            promptGujarati = "દોરો",
            evaluationMode = EvaluationMode.PARTICIPATION,
        )
        assertEquals(AttemptResult.UNKNOWN, ActivityEvaluator.evaluate(activity, "done"))
    }
}
