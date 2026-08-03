package com.mitra.learning.books.analysis

data class RenderedBookPage(
    val pageNumber: Int,
    val jpegBytes: ByteArray = ByteArray(0),
    val extractedText: String? = null,
    val extractionMethod: String? = null,
)

data class ChapterDraft(
    val id: String,
    val chapterNumber: Int?,
    val titleGujarati: String,
    val titleEnglish: String? = null,
    val startPage: Int,
    val endPage: Int,
)

data class TocAnalysisRequest(
    val bookId: String,
    val bookTitle: String,
    val subject: String,
    val pageCount: Int,
    val pages: List<RenderedBookPage>,
)

data class TocChapterSuggestion(
    val chapterNumber: Int?,
    val titleGujarati: String,
    val titleEnglish: String? = null,
    val startPage: Int,
)

data class TocAnalysisResult(
    val chapters: List<TocChapterSuggestion>,
    val sourceLabel: String,
)

data class ChapterAnalysisRequest(
    val bookId: String,
    val bookTitle: String,
    val subject: String,
    val chapterId: String,
    val chapterTitleGujarati: String,
    val startPage: Int,
    val endPage: Int,
    val pages: List<RenderedBookPage>,
)

data class PageKnowledgeDraft(
    val pageNumber: Int,
    val summaryGujarati: String,
    val visibleTextGujarati: String? = null,
    val importantObjectsJson: String? = null,
    val exercisesJson: String? = null,
    val conceptsJson: String? = null,
)

data class ConceptDraft(
    val titleGujarati: String,
    val titleEnglish: String? = null,
    val descriptionGujarati: String,
    val difficulty: Int = 1,
    val expectedLearningOutcome: String,
    val sourcePageStart: Int,
    val sourcePageEnd: Int,
    val practiceReady: Boolean = false,
)

data class ChapterAnalysisResult(
    val pages: List<PageKnowledgeDraft>,
    val concepts: List<ConceptDraft>,
    val sourceLabel: String,
)
