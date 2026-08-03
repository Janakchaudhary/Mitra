package com.mitra.learning.books.text

enum class TextExtractionMethod {
    EMBEDDED_PDF_TEXT,
    TESSERACT_OCR,
    NO_READABLE_TEXT,
}

data class ExtractedBookPage(
    val pageNumber: Int,
    val text: String,
    val method: TextExtractionMethod,
)

interface OfflinePageTextExtractor {
    /**
     * Extracts 1-based page text for the supplied 0-based PDF page indices.
     * Implementations should keep processing completely on-device.
     */
    suspend fun extract(path: String, pageIndices: List<Int>): List<ExtractedBookPage>
}
