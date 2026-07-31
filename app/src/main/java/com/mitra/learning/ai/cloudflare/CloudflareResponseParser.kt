package com.mitra.learning.ai.cloudflare

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object CloudflareResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Extracts final assistant text from either Cloudflare's OpenAI-compatible
     * Chat Completions response or the native Workers AI REST envelope.
     *
     * Supported examples:
     *   {"choices":[{"message":{"content":"..."}}]}
     *   {"choices":[{"message":{"content":[{"type":"text","text":"..."}]}}]}
     *   {"result":{"response":"..."},"success":true}
     */
    fun messageText(body: String): String {
        val root = parseRoot(body)

        openAiCompatibleText(root)?.let { return it }
        nativeWorkersAiText(root)?.let { return it }

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
                // A few compatibility layers expose structured output as parsed JSON.
                (message["parsed"] as? JsonObject)?.let { return it.toString() }
            }
            // Defensive support for completion-like responses.
            contentText(choice["text"])?.let { return it }
        }
        return null
    }

    private fun nativeWorkersAiText(root: JsonObject): String? {
        val result = root["result"]
        when (result) {
            is JsonObject -> {
                contentText(result["response"])?.let { return it }
                contentText(result["text"])?.let { return it }
                contentText(result["content"])?.let { return it }
            }
            else -> contentText(result)?.let { return it }
        }
        contentText(root["response"])?.let { return it }
        return null
    }

    private fun contentText(element: JsonElement?): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull?.takeIf { it.isNotBlank() }
        is JsonObject -> {
            contentText(element["text"])
                ?: contentText(element["content"])
                ?: contentText(element["response"])
        }
        is JsonArray -> element.asSequence()
            .mapNotNull { part -> contentText(part) }
            .filter { it.isNotBlank() }
            .joinToString("")
            .takeIf { it.isNotBlank() }
        else -> null
    }

    private fun parseRoot(body: String): JsonObject =
        json.parseToJsonElement(body) as? JsonObject ?: error("Cloudflare response was not a JSON object")

    private fun String.removeCodeFence(): String {
        if (!startsWith("```")) return this
        return lines()
            .drop(1)
            .dropLastWhile { it.trim() == "```" }
            .joinToString("\n")
            .trim()
    }

    /** Gemma can occasionally add a short sentence around JSON despite a JSON-only prompt. */
    private fun String.extractJsonObject(): String {
        if (startsWith("{") && endsWith("}")) return this
        val start = indexOf('{')
        val end = lastIndexOf('}')
        return if (start >= 0 && end > start) substring(start, end + 1) else this
    }
}
