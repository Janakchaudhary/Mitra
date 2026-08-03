package com.mitra.learning.study.practice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MitraPracticeEvaluatorTest {
    @Test
    fun `accepts spoken numeric answer`() {
        val challenge = MitraVoiceChallenge(
            id = "table-5-6",
            topic = MitraPracticeTopic.TABLES,
            kind = MitraChallengeKind.TABLE,
            promptGujarati = "૫ ગુણ્યા ૬ કેટલા?",
            evaluationMode = MitraChallengeEvaluationMode.NUMERIC,
            expectedNumber = 30,
            correctionGujarati = "૫ ગુણ્યા ૬ = ૩૦",
        )

        assertTrue(MitraPracticeEvaluator.evaluate(challenge, "ત્રીસ", 0).correct)
        assertFalse(MitraPracticeEvaluator.evaluate(challenge, "પાંત્રીસ", 0).correct)
    }

    @Test
    fun `accepts spelling spoken as letter names`() {
        val challenge = MitraVoiceChallenge(
            id = "spelling-cat",
            topic = MitraPracticeTopic.SPELLING,
            kind = MitraChallengeKind.SPELLING,
            promptGujarati = "CAT નો spelling બોલો.",
            evaluationMode = MitraChallengeEvaluationMode.EXACT_TEXT,
            expectedText = "cat",
            correctionGujarati = "C - A - T",
        )

        assertTrue(MitraPracticeEvaluator.evaluate(challenge, "see ay tee", 2).correct)
    }

    @Test
    fun `keyword answer accepts a prepared book concept word`() {
        val challenge = MitraVoiceChallenge(
            id = "book-water",
            topic = MitraPracticeTopic.BOOK,
            kind = MitraChallengeKind.BOOK,
            promptGujarati = "પાઠનો મુખ્ય વિષય કયો છે?",
            evaluationMode = MitraChallengeEvaluationMode.KEYWORD,
            expectedText = "પાણીનું મહત્વ",
            acceptedAnswers = listOf("પાણી", "મહત્વ"),
            correctionGujarati = "પાણીનું મહત્વ",
        )

        assertTrue(MitraPracticeEvaluator.evaluate(challenge, "આ પાઠ પાણી વિશે છે", 0).correct)
    }
}
