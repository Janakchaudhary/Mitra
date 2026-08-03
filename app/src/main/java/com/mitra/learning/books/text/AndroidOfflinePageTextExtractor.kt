package com.mitra.learning.books.text

import android.content.Context
import com.mitra.learning.books.pdf.PdfPageRenderer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline extraction pipeline:
 * 1. Read selectable/embedded PDF text with PDFBox.
 * 2. Render and OCR only pages whose embedded text is missing or unusable.
 */
class AndroidOfflinePageTextExtractor(
    context: Context,
    private val renderer: PdfPageRenderer,
    private val ocr: TesseractOcrEngine,
) : OfflinePageTextExtractor {
    private val appContext = context.applicationContext

    init {
        PDFBoxResourceLoader.init(appContext)
    }

    override suspend fun extract(path: String, pageIndices: List<Int>): List<ExtractedBookPage> {
        val indices = pageIndices.distinct().sorted()
        if (indices.isEmpty()) return emptyList()

        val embedded = extractEmbeddedText(path, indices)
        return indices.map { index ->
            val text = embedded[index].orEmpty().normalizeRecognizedText()
            if (text.isUsefulBookText()) {
                ExtractedBookPage(
                    pageNumber = index + 1,
                    text = text,
                    method = TextExtractionMethod.EMBEDDED_PDF_TEXT,
                )
            } else {
                val bitmap = renderer.render(path, index, OCR_RENDER_WIDTH_PX)
                try {
                    val recognized = ocr.recognize(bitmap)
                    if (recognized.isUsefulBookText()) {
                        ExtractedBookPage(
                            pageNumber = index + 1,
                            text = recognized,
                            method = TextExtractionMethod.TESSERACT_OCR,
                        )
                    } else {
                        // Picture-only and decorative pages are valid textbook pages. Keep the
                        // page in the prepared chapter instead of failing the entire operation.
                        ExtractedBookPage(
                            pageNumber = index + 1,
                            text = "",
                            method = TextExtractionMethod.NO_READABLE_TEXT,
                        )
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    private suspend fun extractEmbeddedText(
        path: String,
        pageIndices: List<Int>,
    ): Map<Int, String> = withContext(Dispatchers.IO) {
        runCatching {
            PDDocument.load(File(path)).use { document ->
                pageIndices.associateWith { index ->
                    if (index !in 0 until document.numberOfPages) return@associateWith ""
                    PDFTextStripper().apply {
                        startPage = index + 1
                        endPage = index + 1
                        sortByPosition = true
                    }.getText(document).orEmpty()
                }
            }
        }.getOrElse { emptyMap() }
    }

    private fun String.isUsefulBookText(): Boolean {
        val compact = filterNot(Char::isWhitespace)
        if (compact.length < 18) return false
        val lettersOrDigits = compact.count { it.isLetterOrDigit() }
        return lettersOrDigits >= 12 && lettersOrDigits.toDouble() / compact.length >= 0.35
    }

    private companion object {
        const val OCR_RENDER_WIDTH_PX = 1800
    }
}
