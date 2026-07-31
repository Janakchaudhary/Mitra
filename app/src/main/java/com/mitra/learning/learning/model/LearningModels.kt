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

data class LearningQuestion(
    val id: String,
    val promptGujarati: String,
    val expectedAnswer: Int? = null,
    val activityType: String = ActivityType.QUESTION.name,
    val evaluationMode: EvaluationMode = if (expectedAnswer != null) EvaluationMode.NUMERIC else EvaluationMode.PARTICIPATION,
    val expectedText: String? = null,
    val acceptedAnswers: List<String> = emptyList(),
    val optionsGujarati: List<String> = emptyList(),
    val hintGujarati: String? = null,
    val completionButtonGujarati: String = "થઈ ગયું",
    val sourcePage: Int? = null,
) {
    val type: ActivityType
        get() = runCatching { ActivityType.valueOf(activityType) }.getOrDefault(ActivityType.QUESTION)

    val requiresAnswer: Boolean
        get() = evaluationMode != EvaluationMode.PARTICIPATION
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
)

data class SessionSummary(
    val conceptTitleGujarati: String,
    val attempts: Int,
    val correct: Int,
    val mastery: Float,
    val assessed: Int = attempts,
    val participationActivities: Int = 0,
)
