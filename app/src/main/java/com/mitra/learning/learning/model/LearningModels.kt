package com.mitra.learning.learning.model

import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity

data class LearningQuestion(
    val id: String,
    val promptGujarati: String,
    val expectedAnswer: Int,
    val activityType: String = "QUESTION",
)

data class SessionPlan(
    val sessionId: String,
    val concept: ConceptEntity,
    val questions: List<LearningQuestion>,
)

data class AnswerFeedback(
    val result: AttemptResult,
    val messageGujarati: String,
    val expectedAnswer: Int,
    val mastery: Float,
)

data class SessionSummary(
    val conceptTitleGujarati: String,
    val attempts: Int,
    val correct: Int,
    val mastery: Float,
)
