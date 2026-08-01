package com.mitra.learning.ai.cloudflare

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudflareResponseParserTest {
    @Test
    fun extractsChatCompletionStructuredJson() {
        val content = """{"chapters":[{"chapterNumber":1}]}"""
        val body = buildJsonObject {
            put("choices", buildJsonArray {
                add(buildJsonObject {
                    put("message", buildJsonObject {
                        put("role", "assistant")
                        put("content", content)
                    })
                })
            })
        }.toString()

        val parsed = CloudflareResponseParser.parseStructuredText(body)
        assertEquals("[{\"chapterNumber\":1}]", parsed["chapters"].toString())
    }

    @Test
    fun extractsNativeWorkersAiEnvelope() {
        val content = """{"grounded":true,"answerGujarati":"હા"}"""
        val body = buildJsonObject {
            put("success", true)
            put("result", buildJsonObject {
                put("response", content)
            })
            put("errors", buildJsonArray { })
        }.toString()

        val parsed = CloudflareResponseParser.parseStructuredText(body)
        assertEquals("true", parsed["grounded"].toString())
        assertEquals("\"હા\"", parsed["answerGujarati"].toString())
    }

    @Test
    fun extractsArrayContentText() {
        val body = buildJsonObject {
            put("choices", buildJsonArray {
                add(buildJsonObject {
                    put("message", buildJsonObject {
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", "OK")
                            })
                        })
                    })
                })
            })
        }.toString()

        assertEquals("OK", CloudflareResponseParser.messageText(body))
    }

    @Test
    fun stripsMarkdownFenceDefensively() {
        val body = buildJsonObject {
            put("choices", buildJsonArray {
                add(buildJsonObject {
                    put("message", buildJsonObject {
                        put("content", "```json\n{\"grounded\":true}\n```")
                    })
                })
            })
        }.toString()

        val parsed = CloudflareResponseParser.parseStructuredText(body)
        assertEquals("true", parsed["grounded"].toString())
    }

    @Test
    fun extractsNestedNativeOutputText() {
        val body = buildJsonObject {
            put("success", true)
            put("result", buildJsonObject {
                put("output", buildJsonArray {
                    add(buildJsonObject {
                        put("message", buildJsonObject {
                            put("content", buildJsonArray {
                                add(buildJsonObject { put("text", "મિત્રનો જવાબ") })
                            })
                        })
                    })
                })
            })
        }.toString()

        assertEquals("મિત્રનો જવાબ", CloudflareResponseParser.messageText(body))
    }

    @Test
    fun removesThinkingBlockBeforeFinalAnswer() {
        val body = buildJsonObject {
            put("result", buildJsonObject {
                put("response", "<think>hidden reasoning</think>\nઅંતિમ જવાબ")
            })
        }.toString()
        assertEquals("અંતિમ જવાબ", CloudflareResponseParser.messageText(body))
    }

    @Test
    fun extractsCloudflareError() {
        val body = """{"success":false,"errors":[{"code":10000,"message":"authentication error"}]}"""
        assertEquals("authentication error", CloudflareResponseParser.errorMessage(body))
    }
}
