package com.mitra.learning.ai.cloudflare

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object CloudflareResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** Extracts final assistant text across Workers AI native and OpenAI-compatible envelopes. */
    fun messageText(body: String): String {
        val root = parseRoot(body)
        openAiCompatibleText(root)?.let { return it.cleanAssistantText() }
        nativeWorkersAiText(root)?.let { return it.cleanAssistantText() }
        deepOutputText(root)?.let { return it.cleanAssistantText() }
        error("Cloudflare response contained no final assistant text")
    }

    fun parseStructuredText(body: String): JsonObject = parseStructuredString(messageText(body))

    fun parseStructuredString(text: String): JsonObject {
        val cleaned = text.trim().removeCodeFence().extractJsonObject()
        return json.parseToJsonElement(cleaned) as? JsonObject
            ?: error("Cloudflare structured output was not a JSON object")
    }

    fun errorMessage(body: String): String? = runCatching {
        val root = parseRoot(body)
        val errors = root["errors"] as? JsonArray
        val first = errors?.firstOrNull() as? JsonObject
        (first?.get("message") as? JsonPrimitive)?.contentOrNull
            ?: ((root["error"] as? JsonObject)?.get("message") as? JsonPrimitive)?.contentOrNull
    }.getOrNull()

    fun stringify(element: JsonElement?): String? = element?.toString()

    private fun openAiCompatibleText(root: JsonObject): String? {
        val choices = root["choices"] as? JsonArray ?: return null
        choices.forEach { choiceElement ->
            val choice = choiceElement as? JsonObject ?: return@forEach
            val message = choice["message"] as? JsonObject
            if (message != null) {
                contentText(message["content"])?.let { return it }
                contentText(message["output_text"])?.let { return it }
                (message["parsed"] as? JsonObject)?.let { return it.toString() }
            }
            contentText(choice["text"])?.let { return it }
        }
        return null
    }

    private fun nativeWorkersAiText(root: JsonObject): String? {
        val result = root["result"]
        when (result) {
            is JsonObject -> {
                PREFERRED_TEXT_KEYS.forEach { key -> contentText(result[key])?.let { return it } }
                openAiCompatibleText(result)?.let { return it }
            }
            else -> contentText(result)?.let { return it }
        }
        PREFERRED_TEXT_KEYS.forEach { key -> contentText(root[key])?.let { return it } }
        return null
    }

    /** Newer Workers AI models can wrap output in result.output/message/content combinations. */
    private fun deepOutputText(root: JsonObject): String? {
        listOf("output", "data", "result").forEach { key ->
            deepPreferredText(root[key], depth = 0)?.let { return it }
        }
        return null
    }

    private fun deepPreferredText(element: JsonElement?, depth: Int): String? {
        if (element == null || depth > 7) return null
        return when (element) {
            is JsonPrimitive -> element.contentOrNull?.takeIf { it.isNotBlank() }
            is JsonArray -> element.mapNotNull { deepPreferredText(it, depth + 1) }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .takeIf { it.isNotBlank() }
            is JsonObject -> {
                PREFERRED_TEXT_KEYS.forEach { key ->
                    contentText(element[key])?.let { return it }
                }
                listOf("message", "delta", "output", "result", "data", "choices").forEach { key ->
                    deepPreferredText(element[key], depth + 1)?.let { return it }
                }
                null
            }
            else -> null
        }
    }

    private fun contentText(element: JsonElement?): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull?.takeIf { it.isNotBlank() }
        is JsonObject -> {
            PREFERRED_TEXT_KEYS.asSequence().mapNotNull { key -> contentText(element[key]) }.firstOrNull()
                ?: openAiCompatibleText(element)
        }
        is JsonArray -> element.asSequence()
            .mapNotNull { part -> contentText(part) }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .takeIf { it.isNotBlank() }
        else -> null
    }

    private fun parseRoot(body: String): JsonObject =
        json.parseToJsonElement(body) as? JsonObject ?: error("Cloudflare response was not a JSON object")

    private fun String.cleanAssistantText(): String = this
        .replace(Regex("(?s)<think>.*?</think>"), "")
        .replace(Regex("(?s)<analysis>.*?</analysis>"), "")
        .trim()
        .takeIf { it.isNotBlank() }
        ?: error("Cloudflare returned reasoning but no final answer")

    private fun String.removeCodeFence(): String {
        if (!startsWith("```")) return this
        return lines().drop(1).dropLastWhile { it.trim() == "```" }.joinToString("\n").trim()
    }

    private fun String.extractJsonObject(): String {
        if (startsWith("{") && endsWith("}")) return this
        val start = indexOf('{')
        val end = lastIndexOf('}')
        return if (start >= 0 && end > start) substring(start, end + 1) else this
    }

    private val PREFERRED_TEXT_KEYS = listOf(
        "response", "output_text", "text", "content", "answer", "generated_text", "completion",
    )
}
