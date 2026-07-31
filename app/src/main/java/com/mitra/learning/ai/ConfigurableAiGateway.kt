package com.mitra.learning.ai

import com.mitra.learning.ai.openai.OpenAiGateway
import com.mitra.learning.ai.openai.OpenAiHttp
import com.mitra.learning.ai.settings.AiProviderType
import com.mitra.learning.ai.settings.AiSettingsRepository
import com.mitra.learning.books.analysis.ChapterAnalysisRequest
import com.mitra.learning.books.analysis.ChapterAnalysisResult
import com.mitra.learning.books.analysis.TocAnalysisRequest
import com.mitra.learning.books.analysis.TocAnalysisResult
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.security.AndroidKeystoreSecretStore
import com.mitra.learning.security.SecretStore
import com.mitra.learning.study.StudyAnswer
import com.mitra.learning.study.StudyQuestionRequest

class ConfigurableAiGateway(
    private val settingsRepository: AiSettingsRepository,
    private val secretStore: SecretStore,
    private val mock: AiGateway = MockAiGateway(),
    private val http: OpenAiHttp = OpenAiHttp(),
) : AiGateway {

    override suspend fun analyzeTableOfContents(request: TocAnalysisRequest): TocAnalysisResult =
        currentGateway().analyzeTableOfContents(request)

    override suspend fun analyzeChapter(request: ChapterAnalysisRequest): ChapterAnalysisResult =
        currentGateway().analyzeChapter(request)

    override suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext?,
    ): List<LearningQuestion> {
        // Built-in Standard 2 skills are deliberately local/deterministic even when a remote
        // provider is enabled. Remote AI is reserved for prepared textbook-derived concepts.
        if (concept.builtIn) return mock.createPracticeQuestions(concept, count, context)
        return currentGateway().createPracticeQuestions(concept, count, context)
    }

    override suspend fun answerStudyQuestion(request: StudyQuestionRequest): StudyAnswer =
        currentGateway().answerStudyQuestion(request)

    override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int?): String =
        OpenAiGateway.localFeedback(result, expectedAnswer)

    suspend fun testConfiguredProvider(): String {
        val config = settingsRepository.getConfig()
        if (config.provider == AiProviderType.MOCK || !config.remoteEnabled) {
            return "Mock AI is active. No internet provider is being used."
        }
        val key = secretStore.readSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY)
            ?.takeIf { it.isNotBlank() }
            ?: error("Enter and save an API key first.")
        return OpenAiGateway(config, key, http).testConnection()
    }

    private suspend fun currentGateway(): AiGateway {
        val config = settingsRepository.getConfig()
        if (config.provider == AiProviderType.MOCK || !config.remoteEnabled) return mock
        val key = secretStore.readSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY)
            ?.takeIf { it.isNotBlank() }
            ?: error("Remote AI is enabled but no API key is configured in Parent settings.")
        return OpenAiGateway(config, key, http)
    }
}
