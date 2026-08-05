package com.mitra.learning.learning.offline

import com.mitra.learning.data.db.entity.PreparedQuestionEntity
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

private val questionJson = Json { ignoreUnknownKeys = true }

fun LearningQuestion.toPreparedQuestionEntity(
    bookId: String,
    chapterId: String,
    conceptIdOverride: String? = conceptId,
    idOverride: String = id,
    difficulty: Int = 1,
): PreparedQuestionEntity = PreparedQuestionEntity(
    id = idOverride,
    bookId = bookId,
    chapterId = chapterId,
    conceptId = conceptIdOverride,
    promptGujarati = promptGujarati,
    spokenPromptGujarati = spokenPromptGujarati,
    speechLanguageTag = speechLanguageTag,
    recognitionLanguageTag = recognitionLanguageTag,
    expectedAnswer = expectedAnswer,
    activityType = activityType,
    evaluationMode = evaluationMode.name,
    expectedText = expectedText,
    acceptedAnswersJson = encodeStringList(acceptedAnswers),
    optionsGujaratiJson = encodeStringList(optionsGujarati),
    hintGujarati = hintGujarati,
    completionButtonGujarati = completionButtonGujarati,
    sourcePage = sourcePage,
    fingerprint = fingerprint,
    difficulty = difficulty.coerceIn(1, 5),
)

fun PreparedQuestionEntity.toLearningQuestion(): LearningQuestion = LearningQuestion(
    id = id,
    promptGujarati = promptGujarati,
    spokenPromptGujarati = spokenPromptGujarati,
    speechLanguageTag = speechLanguageTag,
    recognitionLanguageTag = recognitionLanguageTag,
    expectedAnswer = expectedAnswer,
    activityType = activityType,
    evaluationMode = runCatching { EvaluationMode.valueOf(evaluationMode) }.getOrDefault(EvaluationMode.SHORT_TEXT),
    expectedText = expectedText,
    acceptedAnswers = decodeStringList(acceptedAnswersJson),
    optionsGujarati = decodeStringList(optionsGujaratiJson),
    hintGujarati = hintGujarati,
    completionButtonGujarati = completionButtonGujarati,
    sourcePage = sourcePage,
    conceptId = conceptId,
)

fun encodeStringList(values: List<String>): String = JsonArray(values.map(::JsonPrimitive)).toString()

fun decodeStringList(value: String): List<String> = runCatching {
    questionJson.parseToJsonElement(value).jsonArray.map { it.jsonPrimitive.content }
}.getOrDefault(emptyList())
