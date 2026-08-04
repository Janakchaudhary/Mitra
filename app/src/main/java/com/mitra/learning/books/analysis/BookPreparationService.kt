package com.mitra.learning.books.analysis

import android.graphics.Bitmap
import com.mitra.learning.ai.AiCapability
import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.PracticeContext
import com.mitra.learning.ai.local.parseOfflineTocLine
import com.mitra.learning.books.pdf.PdfPageRenderer
import com.mitra.learning.books.text.OfflinePageTextExtractor
import com.mitra.learning.data.db.dao.BookDao
import com.mitra.learning.data.db.entity.BookAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.PageKnowledgeEntity
import com.mitra.learning.data.repository.BookKnowledgeRepository
import com.mitra.learning.data.repository.BookRepository
import com.mitra.learning.learning.offline.OfflineQuestionBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

sealed interface BookPreparationResult {
    data class Success(val sourceLabel: String) : BookPreparationResult
    data class Failure(val message: String) : BookPreparationResult
}

class BookPreparationService(
    private val bookRepository: BookRepository,
    private val knowledgeRepository: BookKnowledgeRepository,
    private val bookDao: BookDao,
    private val pdfRenderer: PdfPageRenderer,
    private val aiGateway: AiGateway,
    private val pageTextExtractor: OfflinePageTextExtractor? = null,
    private val questionBank: OfflineQuestionBank? = null,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    companion object {
        const val PREPARATION_UNAVAILABLE_MESSAGE =
            "The selected AI provider cannot prepare this PDF. Select Offline Local, OpenAI or Cloudflare in Parent settings."
        const val OFFLINE_TEXT_EXTRACTOR_UNAVAILABLE_MESSAGE =
            "Offline PDF text/OCR is not available in this build."
    }

    suspend fun detectChapters(bookId: String, tocPageIndices: List<Int>): Result<Pair<List<ChapterDraft>, String>> = runCatching {
        val imageAnalysis = aiGateway.supports(AiCapability.TABLE_OF_CONTENTS_IMAGE_ANALYSIS)
        val textAnalysis = aiGateway.supports(AiCapability.TABLE_OF_CONTENTS_TEXT_ANALYSIS)
        require(imageAnalysis || textAnalysis) { PREPARATION_UNAVAILABLE_MESSAGE }
        val book = requireNotNull(bookRepository.getBook(bookId)) { "Book not found" }
        require(tocPageIndices.isNotEmpty()) { "Choose at least one contents page" }
        val indices = tocPageIndices.distinct().sorted().onEach { index ->
            require(index in 0 until book.pageCount) { "Page ${index + 1} is outside this PDF" }
        }
        val pages = prepareInputs(
            path = book.localPdfPath,
            pageIndices = indices,
            imageAnalysis = imageAnalysis,
            textAnalysis = textAnalysis,
        )
        val result = aiGateway.analyzeTableOfContents(
            TocAnalysisRequest(
                bookId = book.id,
                bookTitle = book.title,
                subject = book.subject,
                pageCount = book.pageCount,
                pages = pages,
            )
        )
        // Contents pages commonly show printed textbook page numbers, while PdfRenderer uses
        // physical PDF pages. When the first detected lesson page points at/before the selected
        // contents page, treat it as a printed number and shift every lesson past the contents.
        // Example: contents is PDF page 12 and lesson 1 says page 1 -> physical PDF page 13.
        val normalizedSuggestions = mapPrintedPagesToPdfPages(
            suggestions = result.chapters,
            selectedTocPageIndices = indices,
            pdfPageCount = book.pageCount,
        )
        val ranged = ChapterRangeResolver.fromStarts(normalizedSuggestions, book.pageCount)
        val drafts = ranged.mapIndexed { index, item ->
            ChapterDraft(
                id = UUID.randomUUID().toString(),
                chapterNumber = item.chapterNumber ?: index + 1,
                titleGujarati = item.titleGujarati,
                titleEnglish = item.titleEnglish,
                startPage = item.startPage,
                endPage = item.endPage,
            )
        }
        drafts to result.sourceLabel
    }


    private fun mapPrintedPagesToPdfPages(
        suggestions: List<TocChapterSuggestion>,
        selectedTocPageIndices: List<Int>,
        pdfPageCount: Int,
    ): List<TocChapterSuggestion> {
        if (suggestions.isEmpty() || selectedTocPageIndices.isEmpty()) return suggestions
        val firstPrintedPage = suggestions.minOf { it.startPage }
        val lastSelectedTocPdfPage = selectedTocPageIndices.maxOrNull()!! + 1 // one-based

        // A genuine physical chapter start should normally be after the contents page. A start
        // at/before it is the strong signal that OCR/model returned the number printed in the book.
        if (firstPrintedPage > lastSelectedTocPdfPage) return suggestions

        val firstPhysicalContentPage = (lastSelectedTocPdfPage + 1).coerceAtMost(pdfPageCount)
        val offset = firstPhysicalContentPage - firstPrintedPage
        if (offset <= 0) return suggestions

        return suggestions.map { item ->
            item.copy(startPage = (item.startPage + offset).coerceIn(1, pdfPageCount))
        }.distinctBy { it.startPage }.sortedBy { it.startPage }
    }

    suspend fun saveChapters(bookId: String, drafts: List<ChapterDraft>): Result<Unit> = runCatching {
        val book = requireNotNull(bookRepository.getBook(bookId)) { "Book not found" }
        val validated = drafts
            .sortedBy { it.startPage }
            .mapIndexed { index, draft ->
                require(draft.titleGujarati.isNotBlank()) { "Chapter ${index + 1} needs a title" }
                require(draft.startPage in 1..book.pageCount) { "Invalid start page" }
                require(draft.endPage in draft.startPage..book.pageCount) { "Invalid end page" }
                ChapterEntity(
                    id = draft.id.ifBlank { UUID.randomUUID().toString() },
                    bookId = bookId,
                    chapterNumber = draft.chapterNumber ?: index + 1,
                    titleGujarati = draft.titleGujarati.trim(),
                    titleEnglish = draft.titleEnglish?.trim()?.takeIf { it.isNotBlank() },
                    startPage = draft.startPage,
                    endPage = draft.endPage,
                    analysisStatus = ChapterAnalysisStatus.NOT_PREPARED,
                )
            }

        for (index in 1 until validated.size) {
            require(validated[index].startPage > validated[index - 1].endPage) {
                "Chapter page ranges overlap"
            }
        }

        val existing = knowledgeRepository.chaptersForBook(bookId).associateBy { it.id }
        val incomingIds = validated.map { it.id }.toSet()
        existing.values.filter { it.id !in incomingIds }.forEach { knowledgeRepository.deleteChapter(it) }
        val finalChapters = validated.map { incoming ->
            val previous = existing[incoming.id]
            val unchanged = previous != null &&
                previous.titleGujarati == incoming.titleGujarati &&
                previous.startPage == incoming.startPage &&
                previous.endPage == incoming.endPage
            if (!unchanged && previous != null) {
                knowledgeRepository.replacePageKnowledge(incoming.id, emptyList())
                knowledgeRepository.replaceChapterConcepts(incoming.id, emptyList())
            }
            if (unchanged) incoming.copy(analysisStatus = previous!!.analysisStatus) else incoming
        }
        knowledgeRepository.upsertChapters(finalChapters)
        val status = if (finalChapters.isNotEmpty() && finalChapters.all { it.analysisStatus == ChapterAnalysisStatus.READY }) {
            BookAnalysisStatus.READY
        } else {
            BookAnalysisStatus.PARTIAL
        }
        bookDao.update(book.copy(analysisStatus = status))
    }

    suspend fun addManualChapter(
        bookId: String,
        titleGujarati: String,
        startPage: Int,
        endPage: Int,
    ): Result<ChapterEntity> = runCatching {
        val book = requireNotNull(bookRepository.getBook(bookId)) { "Book not found" }
        require(titleGujarati.isNotBlank()) { "Chapter title is required" }
        require(startPage in 1..book.pageCount && endPage in startPage..book.pageCount) { "Invalid page range" }
        val existing = knowledgeRepository.chaptersForBook(bookId)
        require(existing.none { startPage <= it.endPage && endPage >= it.startPage }) { "Chapter overlaps an existing range" }
        val chapter = ChapterEntity(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            chapterNumber = (existing.mapNotNull { it.chapterNumber }.maxOrNull() ?: 0) + 1,
            titleGujarati = titleGujarati.trim(),
            titleEnglish = null,
            startPage = startPage,
            endPage = endPage,
            analysisStatus = ChapterAnalysisStatus.NOT_PREPARED,
        )
        knowledgeRepository.upsertChapter(chapter)
        bookDao.update(book.copy(analysisStatus = BookAnalysisStatus.PARTIAL))
        chapter
    }

    suspend fun prepareChapter(chapterId: String): BookPreparationResult {
        val originalChapter = knowledgeRepository.getChapter(chapterId)
            ?: return BookPreparationResult.Failure("Chapter not found")
        val book = bookRepository.getBook(originalChapter.bookId)
            ?: return BookPreparationResult.Failure("Book not found")

        val imageAnalysis = aiGateway.supports(AiCapability.CHAPTER_IMAGE_ANALYSIS)
        val textAnalysis = aiGateway.supports(AiCapability.CHAPTER_TEXT_ANALYSIS)
        // Range repair uses the always-local PDF/OCR extractor and is independent of the
        // selected answer provider, so cloud and offline preparation receive the same pages.
        val repairedChapters = autoRepairGenericChapterRanges(book.id, book.localPdfPath, book.pageCount)
        val chapter = repairedChapters.firstOrNull { it.id == chapterId }
            ?: knowledgeRepository.getChapter(chapterId)
            ?: originalChapter
        if (!imageAnalysis && !textAnalysis) {
            return BookPreparationResult.Failure(PREPARATION_UNAVAILABLE_MESSAGE)
        }

        val statusBeforePreparation = chapter.analysisStatus
        knowledgeRepository.setChapterStatus(chapterId, ChapterAnalysisStatus.PREPARING)
        return try {
            val allPages = mutableListOf<PageKnowledgeEntity>()
            val allConcepts = mutableListOf<ConceptEntity>()
            val sourceLabels = linkedSetOf<String>()
            val pageIndices = (chapter.startPage..chapter.endPage).map { it - 1 }

            val chunkSize = 4
            pageIndices.chunked(chunkSize).forEachIndexed { chunkIndex, chunk ->
                val inputs = prepareInputs(
                    path = book.localPdfPath,
                    pageIndices = chunk,
                    imageAnalysis = imageAnalysis,
                    textAnalysis = textAnalysis,
                )
                val result = aiGateway.analyzeChapter(
                    ChapterAnalysisRequest(
                        bookId = book.id,
                        bookTitle = book.title,
                        subject = book.subject,
                        chapterId = chapter.id,
                        chapterTitleGujarati = chapter.titleGujarati,
                        startPage = chapter.startPage,
                        endPage = chapter.endPage,
                        pages = inputs,
                    )
                )
                sourceLabels += result.sourceLabel
                allPages += result.pages.map { page ->
                    PageKnowledgeEntity(
                        id = "${book.id}:${page.pageNumber}",
                        bookId = book.id,
                        chapterId = chapter.id,
                        pageNumber = page.pageNumber,
                        summaryGujarati = page.summaryGujarati,
                        visibleTextGujarati = page.visibleTextGujarati,
                        importantObjectsJson = page.importantObjectsJson,
                        exercisesJson = page.exercisesJson,
                        conceptsJson = page.conceptsJson,
                        analyzedAt = now(),
                    )
                }
                allConcepts += result.concepts.mapIndexed { conceptIndex, concept ->
                    ConceptEntity(
                        id = "book:${book.id}:chapter:${chapter.id}:$chunkIndex:$conceptIndex",
                        subject = book.subject,
                        standard = book.standard,
                        language = book.language,
                        titleGujarati = concept.titleGujarati,
                        titleEnglish = concept.titleEnglish,
                        descriptionGujarati = concept.descriptionGujarati,
                        difficulty = concept.difficulty.coerceIn(1, 5),
                        expectedLearningOutcome = concept.expectedLearningOutcome,
                        sortOrder = 10_000 + chapter.startPage * 100 + chunkIndex * 10 + conceptIndex,
                        builtIn = false,
                        bookId = book.id,
                        chapterId = chapter.id,
                        sourcePageStart = concept.sourcePageStart,
                        sourcePageEnd = concept.sourcePageEnd,
                        practiceReady = concept.practiceReady,
                    )
                }
            }

            knowledgeRepository.replacePageKnowledge(chapter.id, allPages.distinctBy { it.pageNumber })
            val previousConceptIds = knowledgeRepository.conceptsForChapter(chapter.id).map { it.id }
            previousConceptIds.forEach { questionBank?.deleteForConcept(it) }
            val mergedConcepts = allConcepts
                .groupBy { it.titleGujarati.trim().lowercase() }
                .values
                .map { group ->
                    val first = group.first()
                    first.copy(
                        sourcePageStart = group.mapNotNull { it.sourcePageStart }.minOrNull(),
                        sourcePageEnd = group.mapNotNull { it.sourcePageEnd }.maxOrNull(),
                    )
                }
            knowledgeRepository.replaceChapterConcepts(chapter.id, mergedConcepts)

            // Prepare a reusable question bank during the same local or remote preparation pass.
            val groundedText = allPages.sortedBy { it.pageNumber }.joinToString("\n\n") { page ->
                buildString {
                    append("Page ${page.pageNumber}: ${page.summaryGujarati}")
                    page.visibleTextGujarati?.takeIf { it.isNotBlank() }?.let { append("\nVisible text: $it") }
                    page.exercisesJson?.takeIf { it.isNotBlank() }?.let { append("\nExercises: $it") }
                }
            }.take(16_000)
            mergedConcepts.filter { it.practiceReady }.take(12).forEach { concept ->
                runCatching {
                    aiGateway.createPracticeQuestions(
                        concept = concept,
                        count = 20,
                        context = PracticeContext(
                            bookTitle = book.title,
                            chapterTitleGujarati = chapter.titleGujarati,
                            groundedBookText = groundedText,
                        ),
                    )
                }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { generated ->
                    questionBank?.save(concept.id, generated.map { it.copy(conceptId = it.conceptId ?: concept.id) })
                }
            }

            knowledgeRepository.setChapterStatus(chapter.id, ChapterAnalysisStatus.READY)
            refreshBookStatus(book.id)
            BookPreparationResult.Success(sourceLabels.joinToString(" + ").ifBlank { "Prepared locally" })
        } catch (t: Throwable) {
            // A failed re-prepare must not destroy the usable READY state or its cached knowledge.
            // New/unprepared chapters still surface FAILED so the parent can retry.
            val failureStatus = if (statusBeforePreparation == ChapterAnalysisStatus.READY) {
                ChapterAnalysisStatus.READY
            } else {
                ChapterAnalysisStatus.FAILED
            }
            knowledgeRepository.setChapterStatus(chapter.id, failureStatus)
            refreshBookStatus(book.id)
            BookPreparationResult.Failure(t.message ?: "Chapter preparation failed")
        }
    }

    /**
     * Repairs the common scanned-textbook mistake where printed lesson pages (1, 16, 32...)
     * were saved as physical PDF pages. A strong contents page (at least three entries) is
     * required, so ordinary manually entered chapter ranges are never guessed over.
     */
    private suspend fun autoRepairGenericChapterRanges(
        bookId: String,
        pdfPath: String,
        pageCount: Int,
    ): List<ChapterEntity> {
        val extractor = pageTextExtractor ?: return emptyList()
        val existing = knowledgeRepository.chaptersForBook(bookId).sortedBy { it.startPage }
        if (existing.isEmpty()) return emptyList()
        val first = existing.first()
        val genericTitles = existing.count { chapter ->
            chapter.titleGujarati.trim().matches(Regex("""(?:પાઠ|અધ્યાય|chapter|lesson)\s*[-:.]?\s*\p{N}+""", RegexOption.IGNORE_CASE))
        }
        if (first.startPage > 2 || genericTitles == 0 || genericTitles * 2 < existing.size) return emptyList()

        val scanIndices = (0 until minOf(pageCount, 20)).toList()
        val pages = runCatching { extractor.extract(pdfPath, scanIndices) }.getOrNull() ?: return emptyList()
        val tocCandidate = pages.map { page ->
            val suggestions = page.text.lineSequence()
                .mapNotNull { line -> parseOfflineTocLine(line, pageCount) }
                .distinctBy { it.startPage }
                .sortedBy { it.startPage }
                .toList()
            page to suggestions
        }.filter { (_, suggestions) -> suggestions.size >= 3 }
            .maxByOrNull { (_, suggestions) -> suggestions.size }
            ?: return emptyList()

        val tocPageNumber = tocCandidate.first.pageNumber
        val printed = tocCandidate.second
        val firstPrintedPage = printed.minOf { it.startPage }
        val offset = (tocPageNumber + 1) - firstPrintedPage
        if (offset <= 0) return emptyList()
        val physicalSuggestions = printed.map { suggestion ->
            suggestion.copy(startPage = (suggestion.startPage + offset).coerceIn(1, pageCount))
        }.distinctBy { it.startPage }.sortedBy { it.startPage }
        val ranges = ChapterRangeResolver.fromStarts(physicalSuggestions, pageCount)
        if (ranges.size < 3 || ranges.first().startPage <= tocPageNumber) return emptyList()

        val updated = existing.mapIndexed { index, chapter ->
            val byNumber = ranges.firstOrNull { it.chapterNumber != null && it.chapterNumber == chapter.chapterNumber }
            val range = byNumber ?: ranges.getOrNull(index) ?: return@mapIndexed chapter
            val changed = chapter.startPage != range.startPage || chapter.endPage != range.endPage ||
                chapter.titleGujarati.matches(Regex("""(?:પાઠ|અધ્યાય)\s*[-:.]?\s*\p{N}+"""))
            if (!changed) chapter else {
                knowledgeRepository.replacePageKnowledge(chapter.id, emptyList())
                knowledgeRepository.conceptsForChapter(chapter.id).forEach { concept -> questionBank?.deleteForConcept(concept.id) }
                knowledgeRepository.replaceChapterConcepts(chapter.id, emptyList())
                chapter.copy(
                    titleGujarati = range.titleGujarati,
                    titleEnglish = range.titleEnglish,
                    startPage = range.startPage,
                    endPage = range.endPage,
                    analysisStatus = ChapterAnalysisStatus.NOT_PREPARED,
                )
            }
        }
        if (updated != existing) {
            knowledgeRepository.upsertChapters(updated)
            bookRepository.getBook(bookId)?.let { bookDao.update(it.copy(analysisStatus = BookAnalysisStatus.PARTIAL)) }
            return updated
        }
        return emptyList()
    }

    private suspend fun refreshBookStatus(bookId: String) {
        val book = bookRepository.getBook(bookId) ?: return
        val chapters = knowledgeRepository.chaptersForBook(bookId)
        val status = when {
            chapters.isEmpty() -> BookAnalysisStatus.NOT_ANALYZED
            chapters.all { it.analysisStatus == ChapterAnalysisStatus.READY } -> BookAnalysisStatus.READY
            chapters.any { it.analysisStatus == ChapterAnalysisStatus.FAILED } -> BookAnalysisStatus.FAILED
            else -> BookAnalysisStatus.PARTIAL
        }
        bookDao.update(book.copy(analysisStatus = status))
    }

    private suspend fun prepareInputs(
        path: String,
        pageIndices: List<Int>,
        imageAnalysis: Boolean,
        textAnalysis: Boolean,
    ): List<RenderedBookPage> {
        // Remote vision providers keep their existing image path. Offline Local uses text only,
        // avoiding JPEG encoding and passing all private page content through the on-device path.
        if (imageAnalysis) return pageIndices.map { renderInput(path, it) }
        require(textAnalysis) { PREPARATION_UNAVAILABLE_MESSAGE }
        val extractor = requireNotNull(pageTextExtractor) { OFFLINE_TEXT_EXTRACTOR_UNAVAILABLE_MESSAGE }
        val extractedByPage = extractor.extract(path, pageIndices).associateBy { it.pageNumber }
        return pageIndices.map { pageIndex ->
            val pageNumber = pageIndex + 1
            val page = requireNotNull(extractedByPage[pageNumber]) {
                "Offline extraction did not return PDF page $pageNumber."
            }
            RenderedBookPage(
                pageNumber = page.pageNumber,
                extractedText = page.text,
                extractionMethod = page.method.name,
            )
        }
    }

    private suspend fun renderInput(path: String, pageIndex: Int): RenderedBookPage = withContext(Dispatchers.Default) {
        val bitmap = pdfRenderer.render(path, pageIndex, 1200)
        try {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            RenderedBookPage(pageNumber = pageIndex + 1, jpegBytes = output.toByteArray())
        } finally {
            bitmap.recycle()
        }
    }
}
