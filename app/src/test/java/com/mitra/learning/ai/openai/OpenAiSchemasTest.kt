package com.mitra.learning.ai.openai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiSchemasTest {
    @Test
    fun strictSchemasRequireEveryTopLevelPropertyAndRejectExtras() {
        listOf(OpenAiSchemas.toc, OpenAiSchemas.chapter, OpenAiSchemas.practiceQuestions).forEach { schema ->
            val properties = schema["properties"] as JsonObject
            val required = schema["required"] as JsonArray
            assertEquals(properties.size, required.size)
            assertTrue(required.map { (it as JsonPrimitive).content }.containsAll(properties.keys))
            assertFalse((schema["additionalProperties"] as JsonPrimitive).content.toBoolean())
        }
    }
}
