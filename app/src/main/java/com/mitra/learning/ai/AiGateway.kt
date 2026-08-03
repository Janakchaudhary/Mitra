package com.mitra.learning.ai

import com.mitra.learning.books.analysis.ChapterAnalysisRequest
import com.mitra.learning.books.analysis.ChapterAnalysisResult
import com.mitra.learning.books.analysis.TocAnalysisRequest
import com.mitra.learning.books.analysis.TocAnalysisResult
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.study.StudyAnswer
import com.mitra.learning.study.StudyQuestionRequest

enum class AiCapability {
    TABLE_OF_CONTENTS_IMAGE_ANALYSIS,
    CHAPTER_IMAGE_ANALYSIS,
    TABLE_OF_CONTENTS_TEXT_ANALYSIS,
    CHAPTER_TEXT_ANALYSIS,
    PRACTICE_GENERATION,
    STUDY_CHAT,
}

interface AiGateway {
    /**
     * Capability checks must be cheap and must not require an API key or network request.
     * Callers use this before rendering PDF pages or changing persistent preparation state.
     */
    suspend fun supports(capability: AiCapability): Boolean = true

    suspend fun analyzeTableOfContents(request: TocAnalysisRequest): TocAnalysisResult

    suspend fun analyzeChapter(request: ChapterAnalysisRequest): ChapterAnalysisResult

    suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext? = null,
    ): List<LearningQuestion>

    suspend fun answerStudyQuestion(request: StudyQuestionRequest): StudyAnswer = StudyAnswer(
        answerGujarati = "આ સવાલનો જવાબ તૈયાર કરેલા પુસ્તકમાંથી હાલમાં આપી શકાયો નહીં.",
        grounded = false,
    )

    fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int?): String
}
