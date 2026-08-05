package com.mitra.learning.books.importing

import com.mitra.learning.learning.model.LearningQuestion

data class PreparedBookPackage(
    val schemaVersion: Int,
    val preparedBy: String?,
    val book: PreparedBookInfo,
    val chapters: List<PreparedChapter>,
)

data class PreparedBookInfo(
    val title: String,
    val subject: String,
    val standard: Int,
    val language: String,
    val pageCount: Int,
    val sourcePdfSha256: String?,
)

data class PreparedChapter(
    val key: String,
    val chapterNumber: Int?,
    val titleGujarati: String,
    val titleEnglish: String?,
    val startPage: Int,
    val endPage: Int,
    val summaryGujarati: String?,
    val pages: List<PreparedPage>,
    val vocabulary: List<PreparedVocabulary>,
    val concepts: List<PreparedConcept>,
)

data class PreparedPage(
    val pageNumber: Int,
    val summaryGujarati: String,
    val visibleTextGujarati: String?,
    val importantObjectsJson: String?,
    val exercisesJson: String?,
    val conceptsJson: String?,
)

data class PreparedVocabulary(
    val word: String,
    val meaningGujarati: String,
    val simpleExplanationGujarati: String?,
    val exampleSentenceGujarati: String?,
    val sourcePage: Int,
    val acceptedVoiceForms: List<String>,
)

data class PreparedConcept(
    val key: String,
    val titleGujarati: String,
    val titleEnglish: String?,
    val descriptionGujarati: String,
    val difficulty: Int,
    val expectedLearningOutcome: String,
    val sourcePageStart: Int,
    val sourcePageEnd: Int,
    val practiceReady: Boolean,
    val questions: List<LearningQuestion>,
)
