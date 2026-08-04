package com.mitra.learning.books.analysis

import android.graphics.Bitmap
import android.net.Uri
import com.mitra.learning.ai.AiCapability
import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.PracticeContext
import com.mitra.learning.books.pdf.PdfPageRenderer
import com.mitra.learning.books.text.ExtractedBookPage
import com.mitra.learning.books.text.OfflinePageTextExtractor
import com.mitra.learning.books.text.TextExtractionMethod
import com.mitra.learning.data.db.dao.BookDao
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.BookAnalysisStatus
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.PageKnowledgeEntity
import com.mitra.learning.data.repository.BookKnowledgeRepository
import com.mitra.learning.data.repository.BookRepository
import com.mitra.learning.data.repository.ImportBookResult
import com.mitra.learning.learning.model.LearningQuestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookPreparationCapabilityTest {
    private val book = BookEntity(
        id = "book-1",
        title = "Gujarati Book",
        subject = "Gujarati",
        standard = 2,
        language = "Gujarati",
        localPdfPath = "/unused/source.pdf",
        sha256 = "hash",
        pageCount = 20,
        coverPath = null,
        createdAt = 1L,
        analysisStatus = BookAnalysisStatus.READY,
    )

    private val readyChapter = ChapterEntity(
        id = "chapter-1",
        bookId = book.id,
        chapterNumber = 1,
        titleGujarati = "પાઠ ૧",
        titleEnglish = null,
        startPage = 2,
        endPage = 5,
        analysisStatus = ChapterAnalysisStatus.READY,
    )

    @Test
    fun unsupportedProviderDoesNotChangeReadyStatusOrRenderPdf() = runTest {
        val knowledge = FakeKnowledgeRepository(readyChapter)
        val renderer = FailingRenderer()
        val service = service(knowledge, renderer, UnsupportedGateway)

        val result = service.prepareChapter(readyChapter.id)

        assertTrue(result is BookPreparationResult.Failure)
        assertEquals(
            BookPreparationService.PREPARATION_UNAVAILABLE_MESSAGE,
            (result as BookPreparationResult.Failure).message,
        )
        assertTrue("No status write should happen for an unsupported provider", knowledge.statusUpdates.isEmpty())
        assertFalse(renderer.renderCalled)
    }

    @Test
    fun unsupportedTocDetectionStopsBeforePdfRendering() = runTest {
        val knowledge = FakeKnowledgeRepository(readyChapter)
        val renderer = FailingRenderer()
        val service = service(knowledge, renderer, UnsupportedGateway)

        val result = service.detectChapters(book.id, listOf(0))

        assertTrue(result.isFailure)
        assertEquals(
            BookPreparationService.PREPARATION_UNAVAILABLE_MESSAGE,
            result.exceptionOrNull()?.message,
        )
        assertFalse(renderer.renderCalled)
    }

    @Test
    fun printedLessonPageOneAfterContentsMapsToPhysicalPdfPageThirteen() = runTest {
        val knowledge = FakeKnowledgeRepository(readyChapter)
        val renderer = FailingRenderer()
        val extractor = FakeTextExtractor()
        val service = service(knowledge, renderer, OfflineTextGateway, extractor)

        val result = service.detectChapters(book.id, listOf(11)).getOrThrow()

        assertEquals(13, result.first.single().startPage)
        assertEquals(listOf(11), extractor.requestedIndices)
        assertFalse(renderer.renderCalled)
    }

    @Test
    fun offlineTextPreparationUsesExtractorWithoutRenderingPdf() = runTest {
        val chapter = readyChapter.copy(startPage = 2, endPage = 2, analysisStatus = ChapterAnalysisStatus.NOT_PREPARED)
        val knowledge = FakeKnowledgeRepository(chapter)
        val renderer = FailingRenderer()
        val extractor = FakeTextExtractor()
        val service = service(knowledge, renderer, OfflineTextGateway, extractor)

        val result = service.prepareChapter(chapter.id)

        assertTrue(result is BookPreparationResult.Success)
        assertEquals(listOf(1), extractor.requestedIndices)
        assertFalse(renderer.renderCalled)
        assertEquals(
            listOf(ChapterAnalysisStatus.PREPARING, ChapterAnalysisStatus.READY),
            knowledge.statusUpdates,
        )
    }

    @Test
    fun failedReprepareRestoresExistingReadyStatus() = runTest {
        val invalidRangeReadyChapter = readyChapter.copy(startPage = 5, endPage = 4)
        val knowledge = FakeKnowledgeRepository(
            chapter = invalidRangeReadyChapter,
            failWhenReplacingPages = true,
        )
        val renderer = FailingRenderer()
        val service = service(knowledge, renderer, SupportedGateway)

        val result = service.prepareChapter(invalidRangeReadyChapter.id)

        assertTrue(result is BookPreparationResult.Failure)
        assertEquals(
            listOf(ChapterAnalysisStatus.PREPARING, ChapterAnalysisStatus.READY),
            knowledge.statusUpdates,
        )
        assertFalse(renderer.renderCalled)
    }

    private fun service(
        knowledge: FakeKnowledgeRepository,
        renderer: FailingRenderer,
        gateway: AiGateway = UnsupportedGateway,
        extractor: OfflinePageTextExtractor? = null,
    ) = BookPreparationService(
        bookRepository = FakeBookRepository(book),
        knowledgeRepository = knowledge,
        bookDao = FakeBookDao(),
        pdfRenderer = renderer,
        aiGateway = gateway,
        pageTextExtractor = extractor,
    )
}

private object UnsupportedGateway : AiGateway {
    override suspend fun supports(capability: AiCapability): Boolean = false

    override suspend fun analyzeTableOfContents(request: TocAnalysisRequest): TocAnalysisResult =
        error("Must not be called")

    override suspend fun analyzeChapter(request: ChapterAnalysisRequest): ChapterAnalysisResult =
        error("Must not be called")

    override suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext?,
    ): List<LearningQuestion> = emptyList()

    override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int?): String = ""
}

private object OfflineTextGateway : AiGateway {
    override suspend fun supports(capability: AiCapability): Boolean = capability in setOf(
        AiCapability.TABLE_OF_CONTENTS_TEXT_ANALYSIS,
        AiCapability.CHAPTER_TEXT_ANALYSIS,
    )

    override suspend fun analyzeTableOfContents(request: TocAnalysisRequest): TocAnalysisResult =
        TocAnalysisResult(
            chapters = listOf(TocChapterSuggestion(1, "પાઠ ૧", startPage = 2)),
            sourceLabel = "Offline test",
        )

    override suspend fun analyzeChapter(request: ChapterAnalysisRequest): ChapterAnalysisResult =
        ChapterAnalysisResult(
            pages = request.pages.map {
                PageKnowledgeDraft(
                    pageNumber = it.pageNumber,
                    summaryGujarati = "સારાંશ",
                    visibleTextGujarati = it.extractedText,
                )
            },
            concepts = listOf(
                ConceptDraft(
                    titleGujarati = "વિચાર",
                    descriptionGujarati = "વર્ણન",
                    expectedLearningOutcome = "શીખવાનું પરિણામ",
                    sourcePageStart = request.startPage,
                    sourcePageEnd = request.endPage,
                    practiceReady = true,
                )
            ),
            sourceLabel = "Offline test",
        )

    override suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext?,
    ): List<LearningQuestion> = emptyList()

    override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int?): String = ""
}

private object SupportedGateway : AiGateway {
    override suspend fun analyzeTableOfContents(request: TocAnalysisRequest): TocAnalysisResult =
        error("Not used")

    override suspend fun analyzeChapter(request: ChapterAnalysisRequest): ChapterAnalysisResult =
        error("Not used")

    override suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext?,
    ): List<LearningQuestion> = emptyList()

    override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int?): String = ""
}

private class FakeTextExtractor : OfflinePageTextExtractor {
    val requestedIndices = mutableListOf<Int>()

    override suspend fun extract(path: String, pageIndices: List<Int>): List<ExtractedBookPage> {
        requestedIndices += pageIndices
        return pageIndices.map {
            ExtractedBookPage(
                pageNumber = it + 1,
                text = "પાઠનો વાંચી શકાય એવો ગુજરાતી લખાણ",
                method = TextExtractionMethod.TESSERACT_OCR,
            )
        }
    }
}

private class FailingRenderer : PdfPageRenderer {
    var renderCalled = false

    override suspend fun pageCount(path: String): Int = 0

    override suspend fun render(path: String, pageIndex: Int, targetWidthPx: Int): Bitmap {
        renderCalled = true
        error("PDF rendering must not start")
    }
}

private class FakeBookRepository(private val book: BookEntity) : BookRepository {
    override fun observeBooks(): Flow<List<BookEntity>> = flowOf(listOf(book))
    override fun observeBook(id: String): Flow<BookEntity?> = flowOf(book.takeIf { it.id == id })
    override suspend fun getBook(id: String): BookEntity? = book.takeIf { it.id == id }
    override suspend fun displayName(uri: Uri): String? = null
    override suspend fun importBook(
        source: Uri,
        title: String,
        subject: String,
        standard: Int,
        language: String,
    ): ImportBookResult = ImportBookResult.Failure("Not used")

    override suspend fun deleteBook(id: String) = Unit
}

private class FakeKnowledgeRepository(
    private val chapter: ChapterEntity,
    private val failWhenReplacingPages: Boolean = false,
) : BookKnowledgeRepository {
    val statusUpdates = mutableListOf<ChapterAnalysisStatus>()

    override fun observeChapters(bookId: String): Flow<List<ChapterEntity>> = flowOf(listOf(chapter))
    override suspend fun chaptersForBook(bookId: String): List<ChapterEntity> = listOf(chapter)
    override suspend fun getChapter(chapterId: String): ChapterEntity? = chapter.takeIf { it.id == chapterId }
    override suspend fun upsertChapter(chapter: ChapterEntity) = Unit
    override suspend fun upsertChapters(chapters: List<ChapterEntity>) = Unit
    override suspend fun deleteChapter(chapter: ChapterEntity) = Unit
    override suspend fun setChapterStatus(chapterId: String, status: ChapterAnalysisStatus) {
        statusUpdates += status
    }

    override suspend fun pageKnowledge(chapterId: String): List<PageKnowledgeEntity> = emptyList()
    override suspend fun replacePageKnowledge(chapterId: String, pages: List<PageKnowledgeEntity>) {
        if (failWhenReplacingPages) error("Simulated persistence failure")
    }
    override suspend fun conceptsForChapter(chapterId: String): List<ConceptEntity> = emptyList()
    override suspend fun replaceChapterConcepts(chapterId: String, concepts: List<ConceptEntity>) = Unit
    override suspend fun setConceptPracticeReady(conceptId: String, ready: Boolean) = Unit
    override suspend fun deleteAllForBook(bookId: String) = Unit
}

private class FakeBookDao : BookDao {
    override fun observeAll(): Flow<List<BookEntity>> = flowOf(emptyList())
    override fun observeById(id: String): Flow<BookEntity?> = flowOf(null)
    override suspend fun findById(id: String): BookEntity? = null
    override suspend fun findBySha256(sha256: String): BookEntity? = null
    override suspend fun getAll(): List<BookEntity> = emptyList()
    override suspend fun insert(book: BookEntity) = Unit
    override suspend fun update(book: BookEntity) = Unit
    override suspend fun delete(book: BookEntity) = Unit
}
