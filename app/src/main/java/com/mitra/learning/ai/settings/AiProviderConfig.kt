package com.mitra.learning.ai.settings

import com.mitra.learning.ai.AiCapability

enum class AiProviderType {
    MOCK,
    OPENAI,
    CLOUDFLARE,
    OFFLINE_LOCAL,
}

data class AiProviderConfig(
    val provider: AiProviderType = AiProviderType.OPENAI,
    val remoteEnabled: Boolean = false,
    val baseUrl: String = DEFAULT_OPENAI_BASE_URL,
    val model: String = DEFAULT_OPENAI_MODEL,
    val cloudflareAccountId: String = "",
) {
    companion object {
        const val DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_OPENAI_MODEL = "gpt-5-mini"
        const val DEFAULT_CLOUDFLARE_MODEL = "@cf/google/gemma-4-26b-a4b-it"

        fun defaultModel(provider: AiProviderType): String = when (provider) {
            AiProviderType.CLOUDFLARE -> DEFAULT_CLOUDFLARE_MODEL
            AiProviderType.OFFLINE_LOCAL -> "local-litertlm"
            AiProviderType.OPENAI, AiProviderType.MOCK -> DEFAULT_OPENAI_MODEL
        }
    }
}

fun AiProviderConfig.supports(capability: AiCapability): Boolean = when (provider) {
    AiProviderType.OFFLINE_LOCAL -> capability !in setOf(
        AiCapability.TABLE_OF_CONTENTS_IMAGE_ANALYSIS,
        AiCapability.CHAPTER_IMAGE_ANALYSIS,
    )
    AiProviderType.MOCK,
    AiProviderType.OPENAI,
    AiProviderType.CLOUDFLARE,
    -> true
}
