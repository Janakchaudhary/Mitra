package com.mitra.learning.ai

import com.mitra.learning.books.analysis.ChapterAnalysisRequest
import com.mitra.learning.books.analysis.ChapterAnalysisResult
import com.mitra.learning.books.analysis.TocAnalysisRequest
import com.mitra.learning.books.analysis.TocAnalysisResult
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.model.LearningQuestion

interface AiGateway {
    suspend fun analyzeTableOfContents(request: TocAnalysisRequest): TocAnalysisResult

    suspend fun analyzeChapter(request: ChapterAnalysisRequest): ChapterAnalysisResult

    suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext? = null,
    ): List<LearningQuestion>

    fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int): String
}
