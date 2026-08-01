package com.mitra.learning.ai.local

import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.MockAiGateway
import com.mitra.learning.ai.PracticeContext
import com.mitra.learning.books.analysis.ChapterAnalysisRequest
import com.mitra.learning.books.analysis.ChapterAnalysisResult
import com.mitra.learning.books.analysis.TocAnalysisRequest
import com.mitra.learning.books.analysis.TocAnalysisResult
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.study.OfflineStudyAnswerer
import com.mitra.learning.study.StudyAnswer
import com.mitra.learning.study.StudyQuestionRequest

/**
 * Third provider: fully local study chat over already-prepared textbook text.
 *
 * A parent-imported LiteRT-LM model improves wording and conversation. If no compatible model is
 * installed (or the phone cannot load it), the grounded extractive fallback still works offline.
 * PDF image preparation remains a cloud/manual task because a text-only local model cannot safely
 * understand arbitrary textbook scans.
 */
class OfflineAiGateway(
    private val model: LiteRtLocalModel,
    private val modelStore: LocalModelStore,
    private val fallback: OfflineStudyAnswerer = OfflineStudyAnswerer(),
    private val mock: MockAiGateway = MockAiGateway(),
) : AiGateway {
    override suspend fun analyzeTableOfContents(request: TocAnalysisRequest): TocAnalysisResult {
        error("Offline Local cannot read new PDF page images yet. Use Cloudflare/OpenAI for preparation, or add chapter ranges manually.")
    }

    override suspend fun analyzeChapter(request: ChapterAnalysisRequest): ChapterAnalysisResult {
        error("Offline Local uses chapters that are already prepared. Prepare this chapter once with Cloudflare/OpenAI, then chat and practise offline.")
    }

    override suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext?,
    ): List<LearningQuestion> = mock.createPracticeQuestions(concept, count, context)

    override suspend fun answerStudyQuestion(request: StudyQuestionRequest): StudyAnswer {
        val groundedFallback = fallback.answer(request)
        if (!modelStore.hasModel() || request.sources.isEmpty()) return groundedFallback

        val sourceText = request.sources.take(5).joinToString("\n\n") {
            "[${it.bookTitle} • p.${it.pageNumber}] ${it.text}"
        }.take(12_000)
        val history = request.recentTurns.takeLast(3).joinToString("\n") {
            "બાળક: ${it.question}\nમિત્ર: ${it.answer}"
        }
        val prompt = """
            બાળકનો સવાલ: ${request.question}

            તાજેતરની વાતચીત:
            $history

            તૈયાર પુસ્તકમાંથી માહિતી:
            $sourceText

            ફક્ત ઉપરની પુસ્તક માહિતી પરથી 1 થી 4 ટૂંકા સરળ ગુજરાતી વાક્યોમાં જવાબ આપો.
            માહિતી ન મળે તો ચોક્કસ લખો: પુસ્તકમાં મળ્યું નથી.
            છેલ્લે એક નાનો વિચારવાનો સવાલ પૂછો. કોઈ વેબસાઇટ કે વ્યક્તિગત માહિતી ન પૂછો.
        """.trimIndent()

        return runCatching {
            val text = model.generate(
                systemInstruction = "તમે ધોરણ ૨ના બાળક માટે સુરક્ષિત, ટૂંકા અને પુસ્તક આધારિત ગુજરાતી અભ્યાસ મિત્ર છો.",
                prompt = prompt,
            ).cleanLocalModelText()
            if (text.isBlank() || text.contains("પુસ્તકમાં મળ્યું નથી")) groundedFallback else StudyAnswer(
                answerGujarati = text.take(700),
                sourceLabels = request.sources.take(3).map { "${it.bookTitle} • p.${it.pageNumber}" }.distinct(),
                grounded = true,
            )
        }.getOrElse { groundedFallback }
    }

    override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int?): String =
        mock.feedbackGujarati(result, expectedAnswer)

    suspend fun testConnection(): String = if (modelStore.hasModel()) {
        model.test()
    } else {
        "Offline Local is ready in extractive mode. Import a .litertlm model for more natural offline conversation."
    }
}

private fun String.cleanLocalModelText(): String = this
    .replace(Regex("(?s)<think>.*?</think>"), "")
    .replace(Regex("(?s)```.*?```"), "")
    .trim()
