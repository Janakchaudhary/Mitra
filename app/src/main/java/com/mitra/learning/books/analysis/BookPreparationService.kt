package com.mitra.learning.books.analysis

import android.graphics.Bitmap
import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.PracticeContext
import com.mitra.learning.books.pdf.PdfPageRenderer
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
    private val questionBank: OfflineQuestionBank? = null,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun detectChapters(bookId: String, tocPageIndices: List<Int>): Result<Pair<List<ChapterDraft>, String>> = runCatching {
        val book = requireNotNull(bookRepository.getBook(bookId)) { "Book not found" }
        require(tocPageIndices.isNotEmpty()) { "Choose at least one contents page" }
        val pages = tocPageIndices.distinct().sorted().map { index ->
            require(index in 0 until book.pageCount) { "Page ${index + 1} is outside this PDF" }
            renderInput(book.localPdfPath, index)
        }
        val result = aiGateway.analyzeTableOfContents(
            TocAnalysisRequest(
                bookId = book.id,
                bookTitle = book.title,
                subject = book.subject,
                pageCount = book.pageCount,
                pages = pages,
            )
        )
        val ranged = ChapterRangeResolver.fromStarts(result.chapters, book.pageCount)
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
        val chapter = knowledgeRepository.getChapter(chapterId)
            ?: return BookPreparationResult.Failure("Chapter not found")
        val book = bookRepository.getBook(chapter.bookId)
            ?: return BookPreparationResult.Failure("Book not found")

        knowledgeRepository.setChapterStatus(chapterId, ChapterAnalysisStatus.PREPARING)
        return try {
            val allPages = mutableListOf<PageKnowledgeEntity>()
            val allConcepts = mutableListOf<ConceptEntity>()
            var sourceLabel = "Unknown"
            val pageIndices = (chapter.startPage..chapter.endPage).map { it - 1 }

            pageIndices.chunked(4).forEachIndexed { chunkIndex, chunk ->
                val inputs = chunk.map { renderInput(book.localPdfPath, it) }
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
                sourceLabel = result.sourceLabel
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
                        sortOrder = 10_000 + chapter.startPage * 10 + conceptIndex,
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

            // Prepare a reusable offline question bank while the parent is already online.
            val groundedText = allPages.sortedBy { it.pageNumber }.joinToString("\n\n") { page ->
                buildString {
                    append("Page ${page.pageNumber}: ${page.summaryGujarati}")
                    page.visibleTextGujarati?.takeIf { it.isNotBlank() }?.let { append("\nVisible text: $it") }
                    page.exercisesJson?.takeIf { it.isNotBlank() }?.let { append("\nExercises: $it") }
                }
            }.take(16_000)
            mergedConcepts.filter { it.practiceReady }.take(8).forEach { concept ->
                runCatching {
                    aiGateway.createPracticeQuestions(
                        concept = concept,
                        count = 8,
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
            BookPreparationResult.Success(sourceLabel)
        } catch (t: Throwable) {
            knowledgeRepository.setChapterStatus(chapter.id, ChapterAnalysisStatus.FAILED)
            refreshBookStatus(book.id)
            BookPreparationResult.Failure(t.message ?: "Chapter preparation failed")
        }
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
