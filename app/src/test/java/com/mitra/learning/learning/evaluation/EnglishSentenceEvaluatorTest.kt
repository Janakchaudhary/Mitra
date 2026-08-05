package com.mitra.learning.learning.evaluation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishSentenceEvaluatorTest {
    @Test fun acceptsNaturalArticleVariation() {
        assertTrue(EnglishSentenceEvaluator.evaluate("The girl is reading a book", "A girl is reading the book").correct)
    }

    @Test fun rejectsBagOfWordsWithWrongOrder() {
        val result = EnglishSentenceEvaluator.evaluate("The girl is reading a book", "book girl reading is")
        assertFalse(result.correct)
        assertTrue(result.orderIssue)
    }

    @Test fun explainsMissingBeVerb() {
        val result = EnglishSentenceEvaluator.evaluate("The girl is reading a book", "girl reading book")
        assertFalse(result.correct)
        assertTrue(result.grammarHint?.contains("is/are") == true)
    }
}
