package com.mitra.learning.learning.assignment

import com.mitra.learning.study.practice.MitraChallengeEvaluationMode
import com.mitra.learning.study.practice.MitraChallengeKind
import com.mitra.learning.study.practice.MitraVoiceChallenge

enum class ParentQuizTopic(val titleGujarati: String) {
    PREPARED_BOOK("પુસ્તકનો પાઠ"),
    SKILL("પસંદ કરેલું કૌશલ્ય"),
    TABLES("ઘડિયા"),
    NUMBER_NEIGHBORS("પહેલાં-પછી"),
    SPELLING("English spelling"),
    MIXED("મિશ્ર કસોટી"),
}

data class ParentQuizQuestion(
    val id: String,
    val promptGujarati: String,
    val spokenPrompt: String,
    val recognitionLanguageTag: String,
    val kind: MitraChallengeKind,
    val evaluationMode: MitraChallengeEvaluationMode,
    val expectedNumber: Int?,
    val expectedText: String?,
    val acceptedAnswers: List<String>,
    val hintGujarati: String?,
    val correctionGujarati: String,
    val sourceLabels: List<String>,
) {
    fun toChallenge() = MitraVoiceChallenge(
        id = id,
        topic = com.mitra.learning.study.practice.MitraPracticeTopic.MIXED,
        kind = kind,
        promptGujarati = promptGujarati,
        spokenPrompt = spokenPrompt,
        recognitionLanguageTag = recognitionLanguageTag,
        evaluationMode = evaluationMode,
        expectedNumber = expectedNumber,
        expectedText = expectedText,
        acceptedAnswers = acceptedAnswers,
        hintGujarati = hintGujarati,
        correctionGujarati = correctionGujarati,
        sourceLabels = sourceLabels,
    )
}

data class ParentQuizPlan(
    val id: String,
    val title: String,
    val topic: ParentQuizTopic,
    val createdAt: Long,
    val questions: List<ParentQuizQuestion>,
    val skillConceptId: String? = null,
    val skillTitleGujarati: String? = null,
    val bookId: String? = null,
    val bookTitle: String? = null,
    val chapterId: String? = null,
    val chapterTitleGujarati: String? = null,
)
