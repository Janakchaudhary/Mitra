package com.mitra.learning.learning.engine

import com.mitra.learning.learning.model.AnswerFeedback
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.learning.model.SessionPlan
import com.mitra.learning.learning.model.SessionSummary

interface LearningEngine {
    suspend fun startSession(questionCount: Int = 6): SessionPlan?

    suspend fun startSkillSession(questionCount: Int = 6): SessionPlan? = startSession(questionCount)

    suspend fun submitAnswer(
        sessionId: String,
        conceptId: String,
        question: LearningQuestion,
        answerText: String,
        hintsUsed: Int = 0,
    ): AnswerFeedback

    suspend fun completeParticipation(
        sessionId: String,
        conceptId: String,
        question: LearningQuestion,
    ): AnswerFeedback

    suspend fun skipQuestion(
        sessionId: String,
        conceptId: String,
        question: LearningQuestion,
    ): AnswerFeedback

    suspend fun completeSession(sessionId: String, conceptTitleGujarati: String): SessionSummary

    suspend fun stopSession(sessionId: String)
}
