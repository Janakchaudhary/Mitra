package com.mitra.learning.ai.openai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object OpenAiSchemas {
    val toc: JsonObject = schemaObject(
        properties = mapOf(
            "chapters" to buildJsonObject {
                put("type", "array")
                put("items", schemaObject(
                    properties = mapOf(
                        "chapterNumber" to type("integer"),
                        "titleGujarati" to type("string"),
                        "titleEnglish" to type("string"),
                        "startPage" to type("integer"),
                    )
                ))
            }
        )
    )

    val chapter: JsonObject = schemaObject(
        properties = mapOf(
            "pages" to buildJsonObject {
                put("type", "array")
                put("items", schemaObject(
                    properties = mapOf(
                        "pageNumber" to type("integer"),
                        "summaryGujarati" to type("string"),
                        "visibleTextGujarati" to type("string"),
                        "importantObjects" to arrayOfStrings(),
                        "exercises" to arrayOfStrings(),
                        "concepts" to arrayOfStrings(),
                    )
                ))
            },
            "concepts" to buildJsonObject {
                put("type", "array")
                put("items", schemaObject(
                    properties = mapOf(
                        "titleGujarati" to type("string"),
                        "titleEnglish" to type("string"),
                        "descriptionGujarati" to type("string"),
                        "difficulty" to type("integer"),
                        "expectedLearningOutcome" to type("string"),
                        "sourcePageStart" to type("integer"),
                        "sourcePageEnd" to type("integer"),
                        "practiceReady" to type("boolean"),
                    )
                ))
            },
        )
    )

    val practiceQuestions: JsonObject = schemaObject(
        properties = mapOf(
            "activities" to buildJsonObject {
                put("type", "array")
                put("items", schemaObject(
                    properties = mapOf(
                        "id" to type("string"),
                        "promptGujarati" to type("string"),
                        "spokenPromptGujarati" to type("string"),
                        "speechLanguageTag" to type("string"),
                        "recognitionLanguageTag" to type("string"),
                        "activityType" to type("string"),
                        "evaluationMode" to type("string"),
                        "expectedAnswer" to type("integer"),
                        "expectedText" to type("string"),
                        "acceptedAnswers" to arrayOfStrings(),
                        "optionsGujarati" to arrayOfStrings(),
                        "hintGujarati" to type("string"),
                        "completionButtonGujarati" to type("string"),
                        "sourcePage" to type("integer"),
                    )
                ))
            }
        )
    )

    private fun schemaObject(properties: Map<String, JsonObject>): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", JsonObject(properties))
        put("required", JsonArray(properties.keys.map(::JsonPrimitive)))
        put("additionalProperties", false)
    }

    private fun type(name: String): JsonObject = buildJsonObject { put("type", name) }

    private fun arrayOfStrings(): JsonObject = buildJsonObject {
        put("type", "array")
        put("items", type("string"))
    }
}
