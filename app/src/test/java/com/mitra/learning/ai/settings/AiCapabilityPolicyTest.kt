package com.mitra.learning.ai.settings

import com.mitra.learning.ai.AiCapability
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCapabilityPolicyTest {
    @Test
    fun offlineLocalUsesTextPreparationAndKeepsStudyFeatures() {
        val config = AiProviderConfig(provider = AiProviderType.OFFLINE_LOCAL)

        assertFalse(config.supports(AiCapability.TABLE_OF_CONTENTS_IMAGE_ANALYSIS))
        assertFalse(config.supports(AiCapability.CHAPTER_IMAGE_ANALYSIS))
        assertTrue(config.supports(AiCapability.TABLE_OF_CONTENTS_TEXT_ANALYSIS))
        assertTrue(config.supports(AiCapability.CHAPTER_TEXT_ANALYSIS))
        assertTrue(config.supports(AiCapability.PRACTICE_GENERATION))
        assertTrue(config.supports(AiCapability.STUDY_CHAT))
    }

    @Test
    fun remoteProvidersSupportPdfImageAndTextAnalysis() {
        listOf(AiProviderType.OPENAI, AiProviderType.CLOUDFLARE).forEach { provider ->
            val config = AiProviderConfig(provider = provider, remoteEnabled = true)
            assertTrue(config.supports(AiCapability.TABLE_OF_CONTENTS_IMAGE_ANALYSIS))
            assertTrue(config.supports(AiCapability.CHAPTER_IMAGE_ANALYSIS))
            assertTrue(config.supports(AiCapability.TABLE_OF_CONTENTS_TEXT_ANALYSIS))
            assertTrue(config.supports(AiCapability.CHAPTER_TEXT_ANALYSIS))
        }
    }
}
