package com.mitra.learning.ai

import com.mitra.learning.books.analysis.ChapterAnalysisRequest
import com.mitra.learning.books.analysis.ChapterAnalysisResult
import com.mitra.learning.books.analysis.ConceptDraft
import com.mitra.learning.books.analysis.PageKnowledgeDraft
import com.mitra.learning.books.analysis.TocAnalysisRequest
import com.mitra.learning.books.analysis.TocAnalysisResult
import com.mitra.learning.books.analysis.TocChapterSuggestion
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.curriculum.Standard2SkillActivityFactory
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.study.StudyAnswer
import com.mitra.learning.study.StudyQuestionRequest

/**
 * Development-only AI implementation.
 *
 * Milestone 4 exercises the complete book preparation pipeline without sending a child's
 * textbook to a remote service. The generated chapter/concept content is intentionally marked
 * as mock and book-derived concepts are not practice-ready. Milestone 5 can replace this class
 * with a remote provider without changing Room, book setup, or the learning engine.
 */
class MockAiGateway : AiGateway {
    override suspend fun analyzeTableOfContents(request: TocAnalysisRequest): TocAnalysisResult {
        val selectedLastPage = request.pages.maxOfOrNull { it.pageNumber } ?: 1
        val firstContentPage = (selectedLastPage + 1).coerceAtMost(request.pageCount)
        val remaining = (request.pageCount - firstContentPage + 1).coerceAtLeast(1)
        val chapterCount = when {
            remaining >= 60 -> 6
            remaining >= 30 -> 4
            remaining >= 12 -> 3
            else -> 1
        }
        val step = (remaining / chapterCount).coerceAtLeast(1)
        val chapters = (0 until chapterCount).map { index ->
            TocChapterSuggestion(
                chapterNumber = index + 1,
                titleGujarati = "પાઠ ${index + 1}",
                titleEnglish = "Chapter ${index + 1}",
                startPage = (firstContentPage + index * step).coerceAtMost(request.pageCount),
            )
        }
        return TocAnalysisResult(chapters, sourceLabel = "Mock analysis — review chapter ranges")
    }

    override suspend fun analyzeChapter(request: ChapterAnalysisRequest): ChapterAnalysisResult {
        val pages = request.pages.map { page ->
            PageKnowledgeDraft(
                pageNumber = page.pageNumber,
                summaryGujarati = "${request.chapterTitleGujarati} — પાનું ${page.pageNumber}. આ પાનું Milestone 5 માં વાસ્તવિક AI દ્વારા સમજાશે.",
                visibleTextGujarati = null,
            )
        }
        val subject = request.subject.lowercase()
        val (title, description, outcome) = when {
            subject.contains("math") || subject.contains("ગણિત") -> Triple(
                request.chapterTitleGujarati,
                "આ પાઠમાં દર્શાવેલી ગણિતની મુખ્ય કલ્પનાઓનો અભ્યાસ.",
                "પાઠના મુખ્ય ગણિતીય વિચારને ઓળખી અને સમજાવી શકે.",
            )
            subject.contains("gujar") || subject.contains("ગુજરાત") -> Triple(
                "વાંચન: ${request.chapterTitleGujarati}",
                "આ પાઠનું વાંચન, શબ્દસમજ અને અર્થ સમજવાનો અભ્યાસ.",
                "પાઠ વાંચીને મુખ્ય શબ્દો અને વિચાર ઓળખી શકે.",
            )
            else -> Triple(
                request.chapterTitleGujarati,
                "આ પાઠના મુખ્ય વિચારનો અભ્યાસ.",
                "પાઠના મુખ્ય વિચારને ઓળખી અને પોતાના શબ્દોમાં કહી શકે.",
            )
        }
        return ChapterAnalysisResult(
            pages = pages,
            concepts = listOf(
                ConceptDraft(
                    titleGujarati = title,
                    descriptionGujarati = description,
                    expectedLearningOutcome = outcome,
                    sourcePageStart = request.startPage,
                    sourcePageEnd = request.endPage,
                    practiceReady = false,
                )
            ),
            sourceLabel = "Mock analysis — concepts saved but not enabled for practice",
        )
    }

    override suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext?,
    ): List<LearningQuestion> = Standard2SkillActivityFactory.create(concept, count)

    override suspend fun answerStudyQuestion(request: StudyQuestionRequest): StudyAnswer = StudyAnswer(
        answerGujarati = "પુસ્તક વિશે વાત કરવા Parent settings માં Remote AI ચાલુ કરો. તૈયાર પુસ્તકોમાંથી જ જવાબ આપવામાં આવશે.",
        grounded = false,
    )

    override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int?): String = when (result) {
        AttemptResult.CORRECT -> "હા! સાચું. તમે કેવી રીતે શોધ્યું તે યાદ રાખજો."
        AttemptResult.INCORRECT -> "ફરી વિચારીએ. વસ્તુઓ ગણીને અથવા આંગળીઓથી અજમાવો." + (expectedAnswer?.let { " સાચો જવાબ $it છે." } ?: "")
        AttemptResult.PARTIAL -> "લગભગ સાચું. એક વાર ફરી ધીમે વિચારીએ."
        AttemptResult.SKIPPED -> "ઠીક છે. આ પ્રશ્ન પછી ફરી અજમાવીશું."
        AttemptResult.UNKNOWN -> "ચાલો આ પ્રશ્ન ફરીથી અજમાવીએ."
    }

}
