package com.mitra.learning.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyLocalResponderTest {
    private val responder = StudyLocalResponder()

    @Test
    fun `two digit addition explains carry locally`() {
        val answer = responder.respond("૨૭ + ૧૮ કેમ કરીએ?")
        assertNotNull(answer)
        assertEquals(StudyResponseKind.LOCAL_MATH, answer?.responseKind)
        assertTrue(answer!!.answerGujarati.contains("કેરી"))
        assertTrue(answer.answerGujarati.contains("45"))
    }

    @Test
    fun `mobile game question gives balanced guidance`() {
        val answer = responder.respond("હું વધુ મોબાઇલ ગેમ રમી શકું?")
        assertEquals(StudyResponseKind.LOCAL_GUIDANCE, answer?.responseKind)
        assertTrue(answer!!.answerGujarati.contains("શીખવાની રમત"))
        assertTrue(answer.answerGujarati.contains("ફોનને આરામ"))
    }

    @Test
    fun `normal textbook question is not intercepted`() {
        assertEquals(null, responder.respond("આ પાઠમાં સિંહ ક્યાં રહે છે?"))
    }
    @Test
    fun `spoken Gujarati number words are handled locally`() {
        val answer = responder.respond("સત્તાવીસ વત્તા અઢાર કેવી રીતે કરીએ?")
        assertEquals(StudyResponseKind.LOCAL_MATH, answer?.responseKind)
        assertTrue(answer!!.answerGujarati.contains("કેરી"))
        assertTrue(answer.answerGujarati.contains("45"))
    }

    @Test
    fun `table request is answered without textbook or cloud`() {
        val answer = responder.respond("સાત નો પહાડો બોલો")
        assertEquals(StudyResponseKind.LOCAL_MATH, answer?.responseKind)
        assertTrue(answer!!.answerGujarati.contains("7 × 10 = 70"))
    }

    @Test
    fun `carry concept question is explained locally`() {
        val answer = responder.respond("કેરી શું છે?")
        assertEquals(StudyResponseKind.LOCAL_MATH, answer?.responseKind)
        assertTrue(answer!!.answerGujarati.contains("૧૦"))
    }

}
