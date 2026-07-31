package com.mitra.learning.ai.openai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object OpenAiResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseRoot(body: String): JsonObject = json.parseToJsonElement(body) as? JsonObject
        ?: error("AI response was not a JSON object")

    fun outputText(root: JsonObject): String {
        // Some clients/adapters may expose the convenience field directly.
        (root["output_text"] as? JsonPrimitive)
            ?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        // Canonical Responses API shape: output[] -> message -> content[] -> output_text.text
        val output = root["output"] as? JsonArray ?: error("AI response has no output")
        output.forEach { item ->
            val itemObject = item as? JsonObject ?: return@forEach
            val content = itemObject["content"] as? JsonArray ?: return@forEach
            content.forEach { part ->
                val partObject = part as? JsonObject ?: return@forEach
                if ((partObject["type"] as? JsonPrimitive)?.contentOrNull == "output_text") {
                    val text = (partObject["text"] as? JsonPrimitive)?.contentOrNull
                    if (!text.isNullOrBlank()) return text
                }
            }
        }
        error("AI response contained no output text")
    }

    fun errorMessage(root: JsonObject): String? {
        val error = root["error"] as? JsonObject ?: return null
        return (error["message"] as? JsonPrimitive)?.contentOrNull
    }

    fun parseStructuredText(body: String): JsonObject {
        val root = parseRoot(body)
        val text = outputText(root)
        return json.parseToJsonElement(text) as? JsonObject
            ?: error("Structured AI output was not a JSON object")
    }

    fun stringify(element: JsonElement?): String? = element?.toString()
}
