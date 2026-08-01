package com.mitra.learning.ai

import com.mitra.learning.ai.cloudflare.CloudflareAiGateway
import com.mitra.learning.ai.openai.OpenAiGateway
import com.mitra.learning.ai.openai.OpenAiHttp
import com.mitra.learning.ai.local.OfflineAiGateway
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
import com.mitra.learning.study.OfflineStudyAnswerer
import com.mitra.learning.study.StudyAnswer
import com.mitra.learning.study.StudyQuestionRequest

class ConfigurableAiGateway(
    private val settingsRepository: AiSettingsRepository,
    private val secretStore: SecretStore,
    private val mock: AiGateway = MockAiGateway(),
    private val openAiHttp: OpenAiHttp = OpenAiHttp(),
    private val offline: OfflineAiGateway,
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
        // Built-in Standard 2 skills remain local/deterministic for speed, privacy and offline use.
        if (concept.builtIn) return mock.createPracticeQuestions(concept, count, context)
        return currentGateway().createPracticeQuestions(concept, count, context)
    }

    override suspend fun answerStudyQuestion(request: StudyQuestionRequest): StudyAnswer =
        runCatching { currentGateway().answerStudyQuestion(request) }
            .getOrElse { OfflineStudyAnswerer().answer(request) }

    override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int?): String =
        OpenAiGateway.localFeedback(result, expectedAnswer)

    suspend fun testConfiguredProvider(): String {
        val config = settingsRepository.getConfig()
        if (config.provider == AiProviderType.OFFLINE_LOCAL) return offline.testConnection()
        if (!config.remoteEnabled || config.provider == AiProviderType.MOCK) {
            return "Offline/mock AI is active. No internet provider is being used."
        }
        return when (config.provider) {
            AiProviderType.OPENAI -> {
                val key = requireSecret(
                    AndroidKeystoreSecretStore.OPENAI_API_KEY,
                    "Enter and save an OpenAI API key first.",
                )
                OpenAiGateway(config, key, openAiHttp).testConnection()
            }
            AiProviderType.CLOUDFLARE -> {
                val token = requireSecret(
                    AndroidKeystoreSecretStore.CLOUDFLARE_API_TOKEN,
                    "Enter and save a Cloudflare Workers AI API token first.",
                )
                CloudflareAiGateway(config, token).testConnection()
            }
            AiProviderType.OFFLINE_LOCAL -> offline.testConnection()
            AiProviderType.MOCK -> "Offline/mock AI is active."
        }
    }

    private suspend fun currentGateway(): AiGateway {
        val config = settingsRepository.getConfig()
        if (config.provider == AiProviderType.OFFLINE_LOCAL) return offline
        if (!config.remoteEnabled || config.provider == AiProviderType.MOCK) return mock
        return when (config.provider) {
            AiProviderType.OPENAI -> OpenAiGateway(
                config = config,
                apiKey = requireSecret(
                    AndroidKeystoreSecretStore.OPENAI_API_KEY,
                    "OpenAI is enabled but no API key is configured in Parent settings.",
                ),
                http = openAiHttp,
            )
            AiProviderType.CLOUDFLARE -> CloudflareAiGateway(
                config = config,
                apiKey = requireSecret(
                    AndroidKeystoreSecretStore.CLOUDFLARE_API_TOKEN,
                    "Cloudflare is enabled but no API token is configured in Parent settings.",
                ),
            )
            AiProviderType.OFFLINE_LOCAL -> offline
            AiProviderType.MOCK -> mock
        }
    }

    private fun requireSecret(key: String, message: String): String =
        secretStore.readSecret(key)?.takeIf { it.isNotBlank() } ?: error(message)
}
