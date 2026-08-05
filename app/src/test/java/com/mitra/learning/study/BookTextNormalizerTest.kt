package com.mitra.learning.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTextNormalizerTest {
    @Test
    fun GujaratiMeaningQuestionKeepsOnlySearchableWord() {
        assertEquals(listOf("દંગોરોનો"), BookTextNormalizer.queryWords("દંગોરોનો અર્થ શું?"))
    }

    @Test
    fun FtsQueryUsesPrefixTermsWithoutStopWords() {
        val query = BookTextNormalizer.ftsQuery("ખિસકોલી ક્યાં રહે છે?")
        assertTrue(query.contains("ખિસકોલી*"))
        assertTrue(query.contains("ક્યાં*"))
        assertFalse(query.contains("છે*"))
    }
}
