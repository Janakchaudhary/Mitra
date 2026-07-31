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
    fun extractsCloudflareError() {
        val body = """{"success":false,"errors":[{"code":10000,"message":"authentication error"}]}"""
        assertEquals("authentication error", CloudflareResponseParser.errorMessage(body))
    }
}
