package com.mitra.learning.books.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterRangeResolverTest {
    @Test
    fun `end page is page before next chapter`() {
        val result = ChapterRangeResolver.fromStarts(
            suggestions = listOf(
                TocChapterSuggestion(1, "એક", startPage = 5),
                TocChapterSuggestion(2, "બે", startPage = 12),
                TocChapterSuggestion(3, "ત્રણ", startPage = 20),
            ),
            pageCount = 30,
        )

        assertEquals(listOf(5, 12, 20), result.map { it.startPage })
        assertEquals(listOf(11, 19, 30), result.map { it.endPage })
    }

    @Test
    fun `invalid and duplicate starts are ignored`() {
        val result = ChapterRangeResolver.fromStarts(
            suggestions = listOf(
                TocChapterSuggestion(1, "bad", startPage = 0),
                TocChapterSuggestion(2, "one", startPage = 3),
                TocChapterSuggestion(3, "duplicate", startPage = 3),
                TocChapterSuggestion(4, "bad2", startPage = 99),
            ),
            pageCount = 10,
        )

        assertEquals(1, result.size)
        assertEquals(3, result.single().startPage)
        assertEquals(10, result.single().endPage)
    }
}
