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
import com.mitra.learning.learning.curriculum.BuiltInCurriculum
import com.mitra.learning.learning.model.LearningQuestion

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
    ): List<LearningQuestion> {
        val base = when (concept.id) {
            BuiltInCurriculum.COUNT_1_20 -> listOf(
                q("c1", "૪ પછી કયો અંક આવે?", 5),
                q("c2", "૯ પછી કયો અંક આવે?", 10),
                q("c3", "૧૪ પછી કયો અંક આવે?", 15),
                q("c4", "૮ પહેલાં કયો અંક આવે?", 7),
                q("c5", "૧૯ પછી કયો અંક આવે?", 20),
            )
            BuiltInCurriculum.COUNT_21_50 -> listOf(
                q("c21", "૨૯ પછી કયો અંક આવે?", 30),
                q("c22", "૩૪ પહેલાં કયો અંક આવે?", 33),
                q("c23", "૪૧ પછી કયો અંક આવે?", 42),
                q("c24", "૨૫ પછી બે અંક ગણો. કયો અંક આવશે?", 27),
                q("c25", "૪૯ પછી કયો અંક આવે?", 50),
            )
            BuiltInCurriculum.ADD_UNDER_10 -> listOf(
                q("a1", "તમારી પાસે ૩ કેરી છે. ૨ વધુ મળે તો કુલ કેટલી?", 5),
                q("a2", "૪ + ૩ કેટલા?", 7),
                q("a3", "૨ પેન્સિલ અને ૬ પેન્સિલ મળીને કેટલી?", 8),
                q("a4", "૫ + ૪ કેટલા?", 9),
                q("a5", "૧ + ૬ કેટલા?", 7),
            )
            BuiltInCurriculum.ADD_UNDER_20 -> listOf(
                q("a21", "૮ + ૫ કેટલા?", 13),
                q("a22", "૯ + ૭ કેટલા?", 16),
                q("a23", "૧૧ + ૪ કેટલા?", 15),
                q("a24", "૬ + ૧૨ કેટલા?", 18),
                q("a25", "૧૦ + ૯ કેટલા?", 19),
            )
            BuiltInCurriculum.SUBTRACT_UNDER_10 -> listOf(
                q("s1", "તમારી પાસે ૭ લાડુ છે. ૨ આપી દો તો કેટલા રહે?", 5),
                q("s2", "૯ - ૩ કેટલા?", 6),
                q("s3", "૮ માંથી ૫ કાઢો. કેટલા રહે?", 3),
                q("s4", "૬ - ૧ કેટલા?", 5),
                q("s5", "૧૦ માંથી ૪ કાઢો. કેટલા રહે?", 6),
            )
            else -> listOf(q("fallback", "૨ + ૨ કેટલા?", 4))
        }
        if (count <= base.size) return base.take(count)
        return List(count) { index -> base[index % base.size].copy(id = "${base[index % base.size].id}-$index") }
    }

    override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int): String = when (result) {
        AttemptResult.CORRECT -> "હા! સાચું. તમે કેવી રીતે શોધ્યું તે યાદ રાખજો."
        AttemptResult.INCORRECT -> "ફરી વિચારીએ. વસ્તુઓ ગણીને અથવા આંગળીઓથી અજમાવો. સાચો જવાબ $expectedAnswer છે."
        AttemptResult.PARTIAL -> "લગભગ સાચું. એક વાર ફરી ધીમે વિચારીએ."
        AttemptResult.SKIPPED -> "ઠીક છે. આ પ્રશ્ન પછી ફરી અજમાવીશું."
        AttemptResult.UNKNOWN -> "ચાલો આ પ્રશ્ન ફરીથી અજમાવીએ."
    }

    private fun q(id: String, text: String, answer: Int) = LearningQuestion(
        id = id,
        promptGujarati = text,
        expectedAnswer = answer,
    )
}
