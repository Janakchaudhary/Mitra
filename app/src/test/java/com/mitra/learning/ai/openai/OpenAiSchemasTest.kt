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
        listOf(OpenAiSchemas.toc, OpenAiSchemas.chapter, OpenAiSchemas.practiceQuestions, OpenAiSchemas.studyAnswer).forEach { schema ->
            val properties = schema["properties"] as JsonObject
            val required = schema["required"] as JsonArray
            assertEquals(properties.size, required.size)
            assertTrue(required.map { (it as JsonPrimitive).content }.containsAll(properties.keys))
            assertFalse((schema["additionalProperties"] as JsonPrimitive).content.toBoolean())
        }
    }
    @Test
    fun practiceSchemaContainsRichActivityFields() {
        val properties = OpenAiSchemas.practiceQuestions["properties"] as JsonObject
        val activities = properties["activities"] as JsonObject
        val item = activities["items"] as JsonObject
        val itemProperties = item["properties"] as JsonObject

        listOf(
            "activityType",
            "spokenPromptGujarati",
            "speechLanguageTag",
            "recognitionLanguageTag",
            "evaluationMode",
            "acceptedAnswers",
            "optionsGujarati",
            "hintGujarati",
            "completionButtonGujarati",
        ).forEach { key -> assertTrue(itemProperties.containsKey(key)) }
    }

    @Test
    fun studyAnswerSchemaContainsGroundingFields() {
        val properties = OpenAiSchemas.studyAnswer["properties"] as JsonObject
        listOf("answerGujarati", "followUpGujarati", "sourceLabels", "grounded")
            .forEach { key -> assertTrue(properties.containsKey(key)) }
    }

}
