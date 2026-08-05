package com.mitra.learning.books.text

import android.content.Context
import com.mitra.learning.books.pdf.PdfPageRenderer
import com.mitra.learning.data.db.dao.RawPageTextDao
import com.mitra.learning.data.db.entity.RawPageTextEntity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.security.MessageDigest
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
    private val rawPageTextDao: RawPageTextDao? = null,
    private val now: () -> Long = { System.currentTimeMillis() },
) : OfflinePageTextExtractor {
    private val appContext = context.applicationContext
    private val cacheRoot = File(appContext.cacheDir, "book_page_text_v1").apply { mkdirs() }

    init {
        PDFBoxResourceLoader.init(appContext)
    }

    override suspend fun extract(path: String, pageIndices: List<Int>): List<ExtractedBookPage> {
        val indices = pageIndices.distinct().sorted()
        if (indices.isEmpty()) return emptyList()

        val sourceKey = sourceKey(path)
        val roomCached = rawPageTextDao?.find(sourceKey, indices.map { it + 1 }).orEmpty()
            .associateBy { it.pageNumber - 1 }
            .mapValues { (_, entity) ->
                ExtractedBookPage(
                    pageNumber = entity.pageNumber,
                    text = entity.text,
                    method = runCatching { TextExtractionMethod.valueOf(entity.extractionMethod) }
                        .getOrDefault(TextExtractionMethod.NO_READABLE_TEXT),
                )
            }
        val fileCached = indices
            .filterNot(roomCached::containsKey)
            .mapNotNull { index -> readCache(path, index)?.let { index to it } }
            .toMap()
        val cached = roomCached + fileCached
        val missing = indices.filterNot(cached::containsKey)
        val embedded = if (missing.isEmpty()) emptyMap() else extractEmbeddedText(path, missing)
        val newlyExtracted = mutableListOf<RawPageTextEntity>()

        val result = indices.map { index ->
            cached[index] ?: run {
                val text = embedded[index].orEmpty().normalizeRecognizedText()
                val page = if (text.isUsefulBookText()) {
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
                writeCache(path, index, page)
                newlyExtracted += RawPageTextEntity(
                    id = "$sourceKey:${index + 1}",
                    sourceKey = sourceKey,
                    pageNumber = index + 1,
                    text = page.text,
                    extractionMethod = page.method.name,
                    extractionVersion = EXTRACTION_VERSION,
                    confidence = null,
                    extractedAt = now(),
                )
                page
            }
        }
        if (newlyExtracted.isNotEmpty()) rawPageTextDao?.upsertAll(newlyExtracted)
        return result
    }



    private fun readCache(path: String, pageIndex: Int): ExtractedBookPage? = runCatching {
        val file = cacheFile(path, pageIndex)
        if (!file.exists()) return null
        val lines = file.readLines()
        if (lines.size < 2) return null
        ExtractedBookPage(
            pageNumber = pageIndex + 1,
            method = TextExtractionMethod.valueOf(lines.first()),
            text = lines.drop(1).joinToString("\n"),
        )
    }.getOrNull()

    private fun writeCache(path: String, pageIndex: Int, page: ExtractedBookPage) {
        runCatching {
            val file = cacheFile(path, pageIndex)
            val temporary = File(file.parentFile, file.name + ".tmp")
            temporary.writeText(page.method.name + "\n" + page.text)
            if (!temporary.renameTo(file)) {
                file.writeText(temporary.readText())
                temporary.delete()
            }
        }
    }

    private fun sourceKey(path: String): String {
        val source = File(path)
        val identity = "$path|${source.length()}|${source.lastModified()}|$EXTRACTION_VERSION"
        return MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun cacheFile(path: String, pageIndex: Int): File {
        val source = File(path)
        val identity = "$path|${source.length()}|${source.lastModified()}|$pageIndex"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(cacheRoot, "$digest.txt")
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
        const val EXTRACTION_VERSION = 2
    }
}
