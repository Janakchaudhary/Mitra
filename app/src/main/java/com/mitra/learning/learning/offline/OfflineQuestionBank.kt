package com.mitra.learning.learning.offline

import android.content.Context
import com.mitra.learning.learning.model.ArithmeticWork
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * File-backed question cache for prepared textbook concepts.
 * It contains no child answers or personal information.
 */
class OfflineQuestionBank(context: Context) {
    private val root = File(context.filesDir, "question_bank").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }
    private val memoryCache = object : LinkedHashMap<String, List<LearningQuestion>>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<LearningQuestion>>?): Boolean = size > 24
    }

    fun save(conceptId: String, questions: List<LearningQuestion>) {
        if (questions.isEmpty()) return
        val existing = loadAll(conceptId)
        val merged = (existing + questions)
            .distinctBy { it.fingerprint }
            .take(80)
        file(conceptId).writeText(
            buildJsonArray { merged.forEach { add(it.toJson()) } }.toString()
        )
        synchronized(memoryCache) { memoryCache[conceptId] = merged }
    }

    fun load(
        conceptId: String,
        count: Int,
        excludedFingerprints: Set<String> = emptySet(),
    ): List<LearningQuestion> = loadAll(conceptId)
        .filter { it.fingerprint !in excludedFingerprints }
        .shuffled()
        .take(count.coerceAtLeast(0))

    fun count(conceptId: String): Int = loadAll(conceptId).size

    fun preview(conceptId: String, count: Int = 3): List<LearningQuestion> =
        loadAll(conceptId).take(count.coerceIn(0, 10))

    fun deleteForConcept(conceptId: String) {
        file(conceptId).delete()
        synchronized(memoryCache) { memoryCache.remove(conceptId) }
    }

    fun clear() {
        root.deleteRecursively()
        root.mkdirs()
        synchronized(memoryCache) { memoryCache.clear() }
    }

    fun directory(): File = root

    private fun loadAll(conceptId: String): List<LearningQuestion> {
        synchronized(memoryCache) { memoryCache[conceptId]?.let { return it } }
        val target = file(conceptId)
        if (!target.exists()) return emptyList()
        val loaded = runCatching {
            json.parseToJsonElement(target.readText()).jsonArray.mapNotNull { it.jsonObject.toQuestionOrNull() }
        }.getOrElse { emptyList() }
        synchronized(memoryCache) { memoryCache[conceptId] = loaded }
        return loaded
    }

    private fun file(conceptId: String): File = File(root, safeName(conceptId) + ".json")

    private fun safeName(value: String): String = value
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .take(140)
        .ifBlank { "concept" }

    private fun LearningQuestion.toJson(): JsonObject = buildJsonObject {
        put("id", id)
        put("promptGujarati", promptGujarati)
        spokenPromptGujarati?.let { put("spokenPromptGujarati", it) }
        speechLanguageTag?.let { put("speechLanguageTag", it) }
        recognitionLanguageTag?.let { put("recognitionLanguageTag", it) }
        expectedAnswer?.let { put("expectedAnswer", it) }
        put("activityType", activityType)
        put("evaluationMode", evaluationMode.name)
        expectedText?.let { put("expectedText", it) }
        put("acceptedAnswers", buildJsonArray { acceptedAnswers.forEach { add(JsonPrimitive(it)) } })
        put("optionsGujarati", buildJsonArray { optionsGujarati.forEach { add(JsonPrimitive(it)) } })
        hintGujarati?.let { put("hintGujarati", it) }
        put("completionButtonGujarati", completionButtonGujarati)
        sourcePage?.let { put("sourcePage", it) }
        conceptId?.let { put("conceptId", it) }
        arithmeticWork?.let { work ->
            put("arithmeticWork", buildJsonObject {
                put("top", work.top)
                put("bottom", work.bottom)
                put("operator", work.operator)
                put("regrouping", work.regrouping)
            })
        }
    }

    private fun JsonObject.toQuestionOrNull(): LearningQuestion? = runCatching {
        val work = get("arithmeticWork")?.jsonObject?.let {
            ArithmeticWork(
                top = it.getValue("top").jsonPrimitive.intOrNull ?: return@let null,
                bottom = it.getValue("bottom").jsonPrimitive.intOrNull ?: return@let null,
                operator = it.getValue("operator").jsonPrimitive.content,
                regrouping = it["regrouping"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        }
        LearningQuestion(
            id = getValue("id").jsonPrimitive.content,
            promptGujarati = getValue("promptGujarati").jsonPrimitive.content,
            spokenPromptGujarati = get("spokenPromptGujarati")?.jsonPrimitive?.content,
            speechLanguageTag = get("speechLanguageTag")?.jsonPrimitive?.content,
            recognitionLanguageTag = get("recognitionLanguageTag")?.jsonPrimitive?.content,
            expectedAnswer = get("expectedAnswer")?.jsonPrimitive?.intOrNull,
            activityType = get("activityType")?.jsonPrimitive?.content ?: "QUESTION",
            evaluationMode = get("evaluationMode")?.jsonPrimitive?.content
                ?.let { runCatching { EvaluationMode.valueOf(it) }.getOrNull() }
                ?: EvaluationMode.PARTICIPATION,
            expectedText = get("expectedText")?.jsonPrimitive?.content,
            acceptedAnswers = get("acceptedAnswers")?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
            optionsGujarati = get("optionsGujarati")?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
            hintGujarati = get("hintGujarati")?.jsonPrimitive?.content,
            completionButtonGujarati = get("completionButtonGujarati")?.jsonPrimitive?.content ?: "થઈ ગયું",
            sourcePage = get("sourcePage")?.jsonPrimitive?.intOrNull,
            conceptId = get("conceptId")?.jsonPrimitive?.content,
            arithmeticWork = work,
        )
    }.getOrNull()
}
