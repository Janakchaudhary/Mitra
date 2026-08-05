package com.mitra.learning.books.importing

import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.ArithmeticWork
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

object PreparedBookPackageParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(text: String): PreparedBookPackage {
        require(text.length <= 25_000_000) { "Prepared-book file is too large" }
        val root = json.parseToJsonElement(text).asObject("root")
        val schemaVersion = root.int("schemaVersion") ?: 1
        require(schemaVersion == 1) { "Unsupported prepared-book schema version: $schemaVersion" }

        val bookObject = root.objectValue("book") ?: error("Missing book information")
        val chaptersArray = root.array("chapters") ?: error("Missing chapters")
        require(chaptersArray.isNotEmpty()) { "Prepared book must contain at least one chapter" }
        require(chaptersArray.size <= 200) { "Prepared book contains too many chapters" }

        val parsedChapters = chaptersArray.mapIndexed { index, element ->
            parseChapter(element.asObject("chapter ${index + 1}"), index)
        }
        val chapterKeys = parsedChapters.map { it.key }
        require(chapterKeys.distinct().size == chapterKeys.size) { "Chapter keys must be unique" }
        parsedChapters.sortedBy { it.startPage }.zipWithNext().forEach { (previous, next) ->
            require(previous.endPage < next.startPage) {
                "Physical PDF chapter ranges overlap: ${previous.startPage}-${previous.endPage} and ${next.startPage}-${next.endPage}"
            }
        }

        val inferredPageCount = parsedChapters.maxOf { it.endPage }
        val pageCount = (bookObject.int("pageCount") ?: inferredPageCount).coerceAtLeast(inferredPageCount)
        val sourceSha = bookObject.string("sourcePdfSha256")
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
        if (sourceSha != null) {
            require(sourceSha.matches(Regex("[0-9a-f]{64}"))) {
                "sourcePdfSha256 must contain exactly 64 hexadecimal characters"
            }
        }

        return PreparedBookPackage(
            schemaVersion = schemaVersion,
            preparedBy = root.string("preparedBy"),
            book = PreparedBookInfo(
                title = bookObject.requiredString("title", "Book title is required"),
                subject = bookObject.requiredString("subject", "Book subject is required"),
                standard = (bookObject.int("standard") ?: 2).coerceIn(1, 12),
                language = bookObject.string("language")?.ifBlank { null } ?: "Gujarati",
                pageCount = pageCount,
                sourcePdfSha256 = sourceSha,
            ),
            chapters = parsedChapters,
        )
    }

    private fun parseChapter(value: JsonObject, index: Int): PreparedChapter {
        val key = value.string("key")?.trim().orEmpty().ifBlank { "chapter-${index + 1}" }
        val startPage = value.int("startPage") ?: error("Chapter ${index + 1} is missing startPage")
        val endPage = value.int("endPage") ?: error("Chapter ${index + 1} is missing endPage")
        require(startPage >= 1 && endPage >= startPage) { "Invalid page range for chapter ${index + 1}" }

        val pages = value.array("pages").orEmpty().mapIndexed { pageIndex, item ->
            val page = item.asObject("chapter ${index + 1}, page ${pageIndex + 1}")
            val pageNumber = page.int("pageNumber") ?: error("A page is missing pageNumber")
            require(pageNumber in startPage..endPage) {
                "Page $pageNumber is outside chapter range $startPage-$endPage"
            }
            val visible = page.string("visibleTextGujarati")
            PreparedPage(
                pageNumber = pageNumber,
                summaryGujarati = page.string("summaryGujarati")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: visible?.take(450)?.ifBlank { null }
                    ?: "પાઠનું તૈયાર જ્ઞાન",
                visibleTextGujarati = visible,
                importantObjectsJson = page.jsonText("importantObjectsJson", "importantObjects"),
                exercisesJson = page.jsonText("exercisesJson", "exercises"),
                conceptsJson = page.jsonText("conceptsJson", "concepts"),
            )
        }

        val vocabulary = value.array("vocabulary").orEmpty().mapIndexed { vocabularyIndex, item ->
            val word = item.asObject("vocabulary ${vocabularyIndex + 1}")
            val sourcePage = word.int("sourcePage") ?: startPage
            require(sourcePage in startPage..endPage) {
                "Vocabulary source page $sourcePage is outside chapter range"
            }
            PreparedVocabulary(
                word = word.requiredString("word", "Vocabulary word is required"),
                meaningGujarati = word.requiredString("meaningGujarati", "Vocabulary meaning is required"),
                simpleExplanationGujarati = word.string("simpleExplanationGujarati"),
                exampleSentenceGujarati = word.string("exampleSentenceGujarati"),
                sourcePage = sourcePage,
                acceptedVoiceForms = word.stringList("acceptedVoiceForms"),
            )
        }

        val concepts = value.array("concepts").orEmpty().mapIndexed { conceptIndex, item ->
            parseConcept(
                value = item.asObject("concept ${conceptIndex + 1}"),
                chapterKey = key,
                chapterStart = startPage,
                chapterEnd = endPage,
                conceptIndex = conceptIndex,
            )
        }
        val conceptKeys = concepts.map { it.key }
        require(conceptKeys.distinct().size == conceptKeys.size) { "Concept keys must be unique inside chapter $key" }

        return PreparedChapter(
            key = key,
            chapterNumber = value.int("chapterNumber"),
            titleGujarati = value.requiredString("titleGujarati", "Gujarati chapter title is required"),
            titleEnglish = value.string("titleEnglish"),
            startPage = startPage,
            endPage = endPage,
            summaryGujarati = value.string("summaryGujarati"),
            pages = pages,
            vocabulary = vocabulary,
            concepts = concepts,
        )
    }

    private fun parseConcept(
        value: JsonObject,
        chapterKey: String,
        chapterStart: Int,
        chapterEnd: Int,
        conceptIndex: Int,
    ): PreparedConcept {
        val key = value.string("key")?.trim().orEmpty().ifBlank { "$chapterKey-concept-${conceptIndex + 1}" }
        val sourceStart = value.int("sourcePageStart") ?: chapterStart
        val sourceEnd = value.int("sourcePageEnd") ?: chapterEnd
        require(sourceStart in chapterStart..chapterEnd && sourceEnd in sourceStart..chapterEnd) {
            "Invalid source-page range for concept $key"
        }
        val questions = value.array("questions").orEmpty()
            .take(250)
            .mapIndexedNotNull { questionIndex, item ->
                parseQuestion(
                    value = item as? JsonObject ?: return@mapIndexedNotNull null,
                    index = questionIndex,
                    chapterStart = chapterStart,
                    chapterEnd = chapterEnd,
                )
            }

        return PreparedConcept(
            key = key,
            titleGujarati = value.requiredString("titleGujarati", "Concept title is required"),
            titleEnglish = value.string("titleEnglish"),
            descriptionGujarati = value.string("descriptionGujarati")
                ?.takeIf { it.isNotBlank() }
                ?: value.requiredString("titleGujarati", "Concept title is required"),
            difficulty = (value.int("difficulty") ?: 1).coerceIn(1, 5),
            expectedLearningOutcome = value.string("expectedLearningOutcome")
                ?.takeIf { it.isNotBlank() }
                ?: "બાળક આ વિચારને સરળ પ્રશ્નોમાં સમજી અને વાપરી શકે.",
            sourcePageStart = sourceStart,
            sourcePageEnd = sourceEnd,
            practiceReady = value.boolean("practiceReady") ?: true,
            questions = questions,
        )
    }

    private fun parseQuestion(
        value: JsonObject,
        index: Int,
        chapterStart: Int,
        chapterEnd: Int,
    ): LearningQuestion? {
        val prompt = value.string("promptGujarati")?.trim().orEmpty()
        if (prompt.isBlank()) return null
        val expectedAnswer = value.int("expectedAnswer")
        val expectedText = value.string("expectedText")
        val acceptedAnswers = value.stringList("acceptedAnswers")
        val options = value.stringList("optionsGujarati")
        val requestedMode = value.string("evaluationMode")
            ?.uppercase()
            ?.let { runCatching { EvaluationMode.valueOf(it) }.getOrNull() }
        val mode = requestedMode ?: when {
            expectedAnswer != null -> EvaluationMode.NUMERIC
            options.isNotEmpty() -> EvaluationMode.MULTIPLE_CHOICE
            expectedText != null || acceptedAnswers.isNotEmpty() -> EvaluationMode.KEYWORD
            else -> EvaluationMode.PARTICIPATION
        }
        val arithmetic = value.objectValue("arithmeticWork")?.let { work ->
            val top = work.int("top") ?: return@let null
            val bottom = work.int("bottom") ?: return@let null
            ArithmeticWork(
                top = top,
                bottom = bottom,
                operator = work.string("operator") ?: "+",
                regrouping = work.boolean("regrouping") ?: false,
            )
        }
        val sourcePage = value.int("sourcePage")
        if (sourcePage != null) {
            require(sourcePage in chapterStart..chapterEnd) {
                "Question source page $sourcePage is outside chapter range $chapterStart-$chapterEnd"
            }
        }
        return LearningQuestion(
            id = value.string("id")?.takeIf { it.isNotBlank() } ?: "import-${index + 1}-${UUID.randomUUID()}",
            promptGujarati = prompt,
            spokenPromptGujarati = value.string("spokenPromptGujarati"),
            speechLanguageTag = value.string("speechLanguageTag"),
            recognitionLanguageTag = value.string("recognitionLanguageTag"),
            expectedAnswer = expectedAnswer,
            activityType = value.string("activityType")
                ?.uppercase()
                ?.let { runCatching { ActivityType.valueOf(it).name }.getOrNull() }
                ?: ActivityType.QUESTION.name,
            evaluationMode = mode,
            expectedText = expectedText,
            acceptedAnswers = acceptedAnswers,
            optionsGujarati = options,
            hintGujarati = value.string("hintGujarati"),
            completionButtonGujarati = value.string("completionButtonGujarati") ?: "થઈ ગયું",
            sourcePage = sourcePage,
            arithmeticWork = arithmetic,
        )
    }

    private fun JsonElement.asObject(label: String): JsonObject = this as? JsonObject
        ?: error("$label must be a JSON object")

    private fun JsonObject.objectValue(name: String): JsonObject? = this[name] as? JsonObject
    private fun JsonObject.array(name: String): JsonArray? = this[name] as? JsonArray
    private fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(name: String): Int? = (this[name] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.boolean(name: String): Boolean? = (this[name] as? JsonPrimitive)?.booleanOrNull
    private fun JsonObject.requiredString(name: String, message: String): String =
        string(name)?.trim()?.takeIf { it.isNotBlank() } ?: error(message)

    private fun JsonObject.stringList(name: String): List<String> =
        (this[name] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotBlank) }
            .orEmpty()

    private fun JsonObject.jsonText(primary: String, alternate: String): String? {
        val value = this[primary] ?: this[alternate] ?: return null
        return when (value) {
            is JsonPrimitive -> value.contentOrNull
            else -> value.toString()
        }
    }
}
