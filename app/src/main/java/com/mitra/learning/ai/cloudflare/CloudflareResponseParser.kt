package com.mitra.learning.ai.cloudflare

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object CloudflareResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun messageText(body: String): String {
        val root = parseRoot(body)
        val choices = root["choices"] as? JsonArray ?: error("Cloudflare response has no choices")
        choices.forEach choiceLoop@ { choice ->
            val message = (choice as? JsonObject)?.get("message") as? JsonObject ?: return@choiceLoop
            (message["content"] as? JsonPrimitive)
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }

            (message["content"] as? JsonArray)?.forEach partLoop@ { part ->
                val partObject = part as? JsonObject ?: return@partLoop
                (partObject["text"] as? JsonPrimitive)
                    ?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
            }

            // Some OpenAI-compatible adapters expose a parsed object for structured output.
            (message["parsed"] as? JsonObject)?.let { return it.toString() }
        }
        error("Cloudflare response contained no assistant text")
    }

    fun parseStructuredText(body: String): JsonObject {
        val text = messageText(body).trim().removeCodeFence()
        return json.parseToJsonElement(text) as? JsonObject
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
}
