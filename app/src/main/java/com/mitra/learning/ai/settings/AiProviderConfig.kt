package com.mitra.learning.ai.settings

enum class AiProviderType {
    MOCK,
    OPENAI,
}

data class AiProviderConfig(
    val provider: AiProviderType = AiProviderType.MOCK,
    val remoteEnabled: Boolean = false,
    val baseUrl: String = DEFAULT_OPENAI_BASE_URL,
    val model: String = DEFAULT_OPENAI_MODEL,
) {
    companion object {
        const val DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_OPENAI_MODEL = "gpt-5-mini"
    }
}
