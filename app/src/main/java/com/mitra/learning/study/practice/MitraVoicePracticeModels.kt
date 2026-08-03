package com.mitra.learning.study.practice

enum class MitraPracticeTopic(
    val titleGujarati: String,
    val emoji: String,
) {
    BOOK("પુસ્તક પ્રશ્ન", "📖"),
    TABLES("ઘડિયા", "✖️"),
    NUMBER_NEIGHBORS("પહેલાં-પછી", "🔢"),
    SPELLING("Spelling", "🔤"),
    MIXED("મિશ્ર રમત", "✨"),
}

enum class MitraChallengeKind {
    BOOK,
    TABLE,
    BEFORE_NUMBER,
    AFTER_NUMBER,
    SPELLING,
}

enum class MitraChallengeEvaluationMode {
    NUMERIC,
    EXACT_TEXT,
    KEYWORD,
}

data class MitraVoiceChallenge(
    val id: String,
    val topic: MitraPracticeTopic,
    val kind: MitraChallengeKind,
    val promptGujarati: String,
    val spokenPrompt: String = promptGujarati,
    val speechLanguageTag: String = "gu-IN",
    val recognitionLanguageTag: String = "gu-IN",
    val evaluationMode: MitraChallengeEvaluationMode,
    val expectedNumber: Int? = null,
    val expectedText: String? = null,
    val acceptedAnswers: List<String> = emptyList(),
    val hintGujarati: String? = null,
    val correctionGujarati: String,
    val sourceLabels: List<String> = emptyList(),
)

data class MitraPracticeEvaluation(
    val correct: Boolean,
    val feedbackGujarati: String,
    val normalizedAnswer: String,
)
