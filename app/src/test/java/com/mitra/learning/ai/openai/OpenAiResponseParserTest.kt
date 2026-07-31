package com.mitra.learning.ai.openai

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenAiResponseParserTest {
    @Test
    fun extractsStructuredOutputText() {
        val structuredText = """{"chapters":[{"chapterNumber":1}]}"""
        val body = buildJsonObject {
            put("id", "resp_123")
            put("output", buildJsonArray {
                add(buildJsonObject {
                    put("type", "message")
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "output_text")
                            put("text", structuredText)
                        })
                    })
                })
            })
        }.toString()

        val structured = OpenAiResponseParser.parseStructuredText(body)
        val chapters = structured["chapters"].toString()
        assertEquals("[{\"chapterNumber\":1}]", chapters)
    }

    @Test
    fun extractsConvenienceOutputText() {
        val body = buildJsonObject {
            put("output_text", """{"chapters":[{"chapterNumber":2}]}""")
        }.toString()

        val structured = OpenAiResponseParser.parseStructuredText(body)
        assertEquals("[{\"chapterNumber\":2}]", structured["chapters"].toString())
    }

    @Test
    fun extractsApiErrorMessage() {
        val root = OpenAiResponseParser.parseRoot(
            """{"error":{"message":"bad key","type":"invalid_request_error"}}"""
        )
        assertEquals("bad key", OpenAiResponseParser.errorMessage(root))
    }
}
