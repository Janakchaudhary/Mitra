package com.mitra.learning.learning.model

import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity

enum class ActivityType {
    QUESTION,
    MULTIPLE_CHOICE,
    RIDDLE,
    STORY,
    BOOK_LOOK,
    READING,
    VOCABULARY,
    SPELLING,
    MISSING_LETTER,
    TABLES,
    WORD_PROBLEM,
    PHYSICAL_MISSION,
    DRAW,
    TEACH_MITRA,
    RECAP,
}

enum class EvaluationMode {
    NUMERIC,
    MULTIPLE_CHOICE,
    SHORT_TEXT,
    KEYWORD,
    PARTICIPATION,
}

/** Metadata used by the finger-writing rough-work board. */
data class ArithmeticWork(
    val top: Int,
    val bottom: Int,
    val operator: String,
    val regrouping: Boolean = false,
)

data class LearningQuestion(
    val id: String,
    val promptGujarati: String,
    val spokenPromptGujarati: String? = null,
    val speechLanguageTag: String? = null,
    val recognitionLanguageTag: String? = null,
    val expectedAnswer: Int? = null,
    val activityType: String = ActivityType.QUESTION.name,
    val evaluationMode: EvaluationMode = if (expectedAnswer != null) EvaluationMode.NUMERIC else EvaluationMode.PARTICIPATION,
    val expectedText: String? = null,
    val acceptedAnswers: List<String> = emptyList(),
    val optionsGujarati: List<String> = emptyList(),
    val hintGujarati: String? = null,
    val completionButtonGujarati: String = "થઈ ગયું",
    val sourcePage: Int? = null,
    /** A mixed skill session records mastery against the question's own concept. */
    val conceptId: String? = null,
    val arithmeticWork: ArithmeticWork? = null,
) {
    val type: ActivityType
        get() = runCatching { ActivityType.valueOf(activityType) }.getOrDefault(ActivityType.QUESTION)

    val requiresAnswer: Boolean
        get() = evaluationMode != EvaluationMode.PARTICIPATION

    val speechTextGujarati: String
        get() = spokenPromptGujarati?.takeIf { it.isNotBlank() } ?: promptGujarati

    /** Stable enough for recent-question suppression; no child data is included. */
    val fingerprint: String
        get() = listOf(
            conceptId.orEmpty(),
            activityType,
            promptGujarati.trim().lowercase().replace(Regex("\\s+"), " "),
            expectedAnswer?.toString().orEmpty(),
            expectedText.orEmpty().trim().lowercase(),
        ).joinToString("|")
}

data class SessionPlan(
    val sessionId: String,
    val concept: ConceptEntity,
    val questions: List<LearningQuestion>,
)

data class AnswerFeedback(
    val result: AttemptResult,
    val messageGujarati: String,
    val expectedAnswer: Int?,
    val mastery: Float,
    val retrySuggested: Boolean = false,
    val mistakeCode: String? = null,
)

data class SessionSummary(
    val conceptTitleGujarati: String,
    val attempts: Int,
    val correct: Int,
    val mastery: Float,
    val assessed: Int = attempts,
    val participationActivities: Int = 0,
)
