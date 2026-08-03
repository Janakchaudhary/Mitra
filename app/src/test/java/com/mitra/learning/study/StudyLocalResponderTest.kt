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


    @Test
    fun `answers Gujarati ghadiya request`() {
        val answer = responder.respond("૫ નો ઘડિયો કહો")
        assertTrue(answer?.answerGujarati.orEmpty().contains("5 × 10 = 50"))
    }

    @Test
    fun `answers English spelling request locally`() {
        val answer = responder.respond("CAT નો spelling શું છે?")
        assertTrue(answer?.answerGujarati.orEmpty().contains("C - A - T"))
    }

    @Test
    fun `answers before and after number wording`() {
        assertTrue(responder.respond("૩૮ પછીની સંખ્યા કઈ?")?.answerGujarati.orEmpty().contains("39"))
        assertTrue(responder.respond("૩૮ પહેલાની સંખ્યા કઈ?")?.answerGujarati.orEmpty().contains("37"))
    }
}
