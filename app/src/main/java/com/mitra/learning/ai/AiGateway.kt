package com.mitra.learning.ai

import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.model.LearningQuestion

interface AiGateway {
    suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
    ): List<LearningQuestion>

    fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int): String
}
