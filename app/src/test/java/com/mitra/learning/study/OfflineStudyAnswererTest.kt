package com.mitra.learning.study

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineStudyAnswererTest {
    private val answerer = OfflineStudyAnswerer()

    @Test
    fun returnsGroundedSentenceFromPreparedSource() {
        val answer = answerer.answer(
            StudyQuestionRequest(
                question = "સિંહ ક્યાં રહે છે?",
                sources = listOf(
                    StudySource(
                        bookTitle = "ગુજરાતી ધોરણ ૨",
                        chapterTitle = "જંગલ",
                        pageNumber = 12,
                        text = "સિંહ જંગલમાં રહે છે. તે શક્તિશાળી પ્રાણી છે.",
                    )
                ),
            )
        )

        assertTrue(answer.grounded)
        assertTrue(answer.answerGujarati.contains("જંગલ"))
        assertTrue(answer.sourceLabels.contains("ગુજરાતી ધોરણ ૨ • p.12"))
    }

    @Test
    fun refusesWhenPreparedSourcesDoNotSupportQuestion() {
        val answer = answerer.answer(
            StudyQuestionRequest(
                question = "ચંદ્ર કેટલો દૂર છે?",
                sources = listOf(
                    StudySource("ગુજરાતી", "ફૂલ", 4, "ગુલાબ લાલ રંગનું ફૂલ છે."),
                ),
            )
        )
        assertFalse(answer.grounded)
        assertTrue(answer.answerGujarati.contains("મળ્યો નથી"))
    }
    @Test
    fun explainsDangoroWhenPreparedPageContainsTheWord() {
        val answer = answerer.answer(
            StudyQuestionRequest(
                question = "દંગોરો નો અર્થ શું?",
                sources = listOf(
                    StudySource(
                        bookTitle = "Std-2 Gujarati First Language",
                        chapterTitle = "ટબૂકડું હું ટાળું",
                        pageNumber = 13,
                        text = "દાદાનો દંગોરો લીધો, એનો તો મેં ઘોડો કીધો.",
                    )
                ),
            )
        )

        assertTrue(answer.grounded)
        assertTrue(answer.answerGujarati.contains("લાંબી અને મજબૂત લાકડી"))
        assertTrue(answer.sourceLabels.contains("Std-2 Gujarati First Language • p.13"))
    }

}
