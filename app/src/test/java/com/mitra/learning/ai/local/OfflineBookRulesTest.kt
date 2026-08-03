package com.mitra.learning.ai.local

import com.mitra.learning.books.analysis.RenderedBookPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class OfflineBookRulesTest {
    @Test
    fun parsesGujaratiDigitsAndDotLeaders() {
        val chapter = parseOfflineTocLine("૧. મારો પરિવાર ........ ૫", pageCount = 40)

        assertEquals(1, chapter?.chapterNumber)
        assertEquals("મારો પરિવાર", chapter?.titleGujarati)
        assertEquals(5, chapter?.startPage)
    }

    @Test
    fun derivesChapterNumberFromGujaratiTitle() {
        val chapter = parseOfflineTocLine("પાઠ ૨ પાણી 12", pageCount = 40)

        assertEquals(2, chapter?.chapterNumber)
        assertEquals("પાઠ 2 પાણી", chapter?.titleGujarati)
        assertEquals(12, chapter?.startPage)
    }

    @Test
    fun rejectsAdministrativeAndOutOfRangeRows() {
        assertNull(parseOfflineTocLine("વિષયસૂચિ ........ 3", pageCount = 40))
        assertNull(parseOfflineTocLine("1. મારો પરિવાર ........ 99", pageCount = 40))
    }

    @Test
    fun pictureOnlyPageIsAcceptedAsAnOfflineTextInput() {
        val pages = listOf(
            RenderedBookPage(
                pageNumber = 7,
                extractedText = "",
                extractionMethod = "NO_READABLE_TEXT",
            )
        )

        assertSame(pages, pages.requireOfflineTextInputs())
    }

    @Test(expected = IllegalArgumentException::class)
    fun imageOnlyInputWithoutExtractionMetadataIsRejected() {
        listOf(RenderedBookPage(pageNumber = 7)).requireOfflineTextInputs()
    }
}
