package com.mitra.learning.ai.openai

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenAiResponseParserTest {
    @Test
    fun extractsStructuredOutputText() {
        val body = """
            {
              "id":"resp_123",
              "output":[
                {
                  "type":"message",
                  "content":[
                    {
                      "type":"output_text",
                      "text":"{\\"chapters\\":[{\\"chapterNumber\\":1}]}"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val structured = OpenAiResponseParser.parseStructuredText(body)
        val chapters = structured["chapters"].toString()
        assertEquals("[{\"chapterNumber\":1}]", chapters)
    }

    @Test
    fun extractsApiErrorMessage() {
        val root = OpenAiResponseParser.parseRoot(
            """{"error":{"message":"bad key","type":"invalid_request_error"}}"""
        )
        assertEquals("bad key", OpenAiResponseParser.errorMessage(root))
    }
}
