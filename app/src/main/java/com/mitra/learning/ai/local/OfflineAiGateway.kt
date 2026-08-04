package com.mitra.learning.ai.local

import com.mitra.learning.ai.AiCapability
import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.MockAiGateway
import com.mitra.learning.ai.PracticeContext
import com.mitra.learning.books.analysis.ChapterAnalysisRequest
import com.mitra.learning.books.analysis.ChapterAnalysisResult
import com.mitra.learning.books.analysis.ConceptDraft
import com.mitra.learning.books.analysis.PageKnowledgeDraft
import com.mitra.learning.books.analysis.RenderedBookPage
import com.mitra.learning.books.analysis.TocAnalysisRequest
import com.mitra.learning.books.analysis.TocAnalysisResult
import com.mitra.learning.books.analysis.TocChapterSuggestion
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.study.OfflineStudyAnswerer
import com.mitra.learning.study.StudyAnswer
import com.mitra.learning.study.StudyQuestionRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Fully local provider.
 *
 * New PDF pages arrive as text extracted on-device by PDFBox or Tesseract OCR. A parent-imported
 * LiteRT-LM model converts that text into chapter/page/concept metadata. When no compatible model
 * is installed, conservative deterministic extraction still creates usable local page knowledge.
 */
class OfflineAiGateway(
    private val model: LiteRtLocalModel,
    private val modelStore: LocalModelStore,
    private val fallback: OfflineStudyAnswerer = OfflineStudyAnswerer(),
    private val mock: MockAiGateway = MockAiGateway(),
) : AiGateway {
    override suspend fun supports(capability: AiCapability): Boolean = when (capability) {
        AiCapability.TABLE_OF_CONTENTS_IMAGE_ANALYSIS,
        AiCapability.CHAPTER_IMAGE_ANALYSIS,
        -> false
        AiCapability.TABLE_OF_CONTENTS_TEXT_ANALYSIS,
        AiCapability.CHAPTER_TEXT_ANALYSIS,
        AiCapability.PRACTICE_GENERATION,
        AiCapability.STUDY_CHAT,
        -> true
    }

    override suspend fun analyzeTableOfContents(request: TocAnalysisRequest): TocAnalysisResult {
        val pages = request.pages.requireOfflineTextInputs()
        val modelResult = if (modelStore.hasModel()) {
            runCatching { analyzeTocWithModel(request, pages) }.getOrNull()
        } else {
            null
        }
        return modelResult ?: analyzeTocDeterministically(request, pages)
    }

    override suspend fun analyzeChapter(request: ChapterAnalysisRequest): ChapterAnalysisResult {
        val pages = request.pages.requireOfflineTextInputs()
        val modelResult = if (modelStore.hasModel()) {
            runCatching { analyzeChapterWithModel(request, pages) }.getOrNull()
        } else {
            null
        }
        return modelResult ?: analyzeChapterDeterministically(request, pages)
    }

    override suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext?,
    ): List<LearningQuestion> {
        val groundedText = context?.groundedBookText?.takeIf { it.isNotBlank() }
            ?: return mock.createPracticeQuestions(concept, count, context)
        val modelQuestions = if (modelStore.hasModel()) {
            runCatching {
                createGroundedPracticeWithModel(concept, count, context, groundedText)
            }.getOrNull()
        } else {
            null
        }
        return modelQuestions?.takeIf { it.isNotEmpty() }
            ?: createGroundedPracticeFallback(concept, count, context)
    }

    private suspend fun createGroundedPracticeWithModel(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext,
        groundedText: String,
    ): List<LearningQuestion> {
        val requested = count.coerceIn(1, 25)
        val prompt = """
            Book: ${context.bookTitle.orEmpty()}
            Chapter: ${context.chapterTitleGujarati.orEmpty()}
            Concept: ${concept.titleGujarati}
            Expected learning outcome: ${concept.expectedLearningOutcome}

            Prepared textbook text:
            ${groundedText.take(14_000)}

            Create $requested short Standard 2 voice-answerable questions using only the supplied textbook text.
            Return STRICT JSON only:
            {"questions":[{
              "id":"q1",
              "promptGujarati":"...",
              "expectedAnswer":null,
              "expectedText":"...",
              "acceptedAnswers":["..."],
              "evaluationMode":"SHORT_TEXT",
              "hintGujarati":"...",
              "sourcePage":1
            }]}

            Rules:
            - Use NUMERIC only when the textbook gives a definite numeric answer.
            - Otherwise use SHORT_TEXT or KEYWORD and include a definite expectedText plus useful acceptedAnswers.
            - Questions and answers must be supported by the supplied text; never invent facts.
            - Keep answers short enough for speech recognition.
            - Do not create participation, drawing, opinion, or open-ended questions.
        """.trimIndent()
        val raw = model.generate(
            systemInstruction = "You create faithful textbook-grounded voice questions for a young child. Output compact valid JSON only.",
            prompt = prompt,
        )
        val root = raw.parseFirstJsonObject()
        return root.array("questions").mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val promptGujarati = item.string("promptGujarati").orEmpty().trim()
            if (promptGujarati.isBlank()) return@mapNotNull null
            val expectedNumber = item.int("expectedAnswer")
            val expectedText = item.string("expectedText")?.trim()?.takeIf { it.isNotBlank() }
            val accepted = item.arrayOrNull("acceptedAnswers")
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotBlank) }
                .orEmpty()
            if (expectedNumber == null && expectedText == null && accepted.isEmpty()) return@mapNotNull null
            val requestedMode = item.string("evaluationMode")?.uppercase()
            val mode = when {
                expectedNumber != null -> EvaluationMode.NUMERIC
                requestedMode == "KEYWORD" -> EvaluationMode.KEYWORD
                else -> EvaluationMode.SHORT_TEXT
            }
            LearningQuestion(
                id = "offline-book-${item.string("id").orEmpty().ifBlank { promptGujarati.hashCode().toString() }}",
                promptGujarati = promptGujarati.take(300),
                spokenPromptGujarati = promptGujarati.take(300),
                recognitionLanguageTag = "gu-IN",
                expectedAnswer = expectedNumber,
                activityType = ActivityType.QUESTION.name,
                evaluationMode = mode,
                expectedText = expectedText,
                acceptedAnswers = (accepted + listOfNotNull(expectedText)).distinct().take(8),
                hintGujarati = item.string("hintGujarati")?.take(220),
                sourcePage = item.int("sourcePage"),
                conceptId = concept.id,
            )
        }.distinctBy { it.fingerprint }.take(requested)
    }

    private fun createGroundedPracticeFallback(
        concept: ConceptEntity,
        count: Int,
        context: PracticeContext,
    ): List<LearningQuestion> {
        val title = concept.titleGujarati.trim().ifBlank { context.chapterTitleGujarati.orEmpty().trim() }
        if (title.isBlank()) return emptyList()
        val keywords = title.lowercase()
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
            .split(' ')
            .filter { it.length >= 2 && it !in setOf("અને", "ના", "ની", "નું") }
            .take(5)
        val page = concept.sourcePageStart
        val questions = listOf(
            LearningQuestion(
                id = "offline-title-${concept.id}",
                promptGujarati = "તૈયાર પાઠનો મુખ્ય વિષય કયો છે?",
                spokenPromptGujarati = "આ પાઠનો મુખ્ય વિષય કયો છે?",
                expectedText = title,
                acceptedAnswers = (listOf(title) + keywords).distinct(),
                evaluationMode = EvaluationMode.KEYWORD,
                activityType = ActivityType.QUESTION.name,
                hintGujarati = "પાઠનું નામ યાદ કરો.",
                sourcePage = page,
                conceptId = concept.id,
            ),
            LearningQuestion(
                id = "offline-outcome-${concept.id}",
                promptGujarati = "આ પાઠમાં આપણે શે વિશે શીખીએ છીએ?",
                spokenPromptGujarati = "આ પાઠમાં આપણે શે વિશે શીખીએ છીએ?",
                expectedText = title,
                acceptedAnswers = (listOf(title) + keywords).distinct(),
                evaluationMode = EvaluationMode.KEYWORD,
                activityType = ActivityType.QUESTION.name,
                hintGujarati = concept.descriptionGujarati.take(180),
                sourcePage = page,
                conceptId = concept.id,
            ),
        )
        return questions.take(count.coerceIn(1, questions.size))
    }

    override suspend fun answerStudyQuestion(request: StudyQuestionRequest): StudyAnswer {
        val groundedFallback = fallback.answer(request)
        if (!modelStore.hasModel() || request.sources.isEmpty()) return groundedFallback

        val sourceText = request.sources.take(5).joinToString("\n\n") {
            "[${it.bookTitle} • p.${it.pageNumber}] ${it.text}"
        }.take(12_000)
        val history = request.recentTurns.takeLast(3).joinToString("\n") {
            "બાળક: ${it.question}\nમિત્ર: ${it.answer}"
        }
        val prompt = """
            બાળકનો સવાલ: ${request.question}

            તાજેતરની વાતચીત:
            $history

            તૈયાર પુસ્તકમાંથી માહિતી:
            $sourceText

            ફક્ત ઉપરની પુસ્તક માહિતી પરથી 1 થી 4 ટૂંકા સરળ ગુજરાતી વાક્યોમાં જવાબ આપો.
            માહિતી ન મળે તો ચોક્કસ લખો: પુસ્તકમાં મળ્યું નથી.
            છેલ્લે એક નાનો વિચારવાનો સવાલ પૂછો. કોઈ વેબસાઇટ કે વ્યક્તિગત માહિતી ન પૂછો.
        """.trimIndent()

        return runCatching {
            val text = model.generate(
                systemInstruction = "તમે ધોરણ ૨ના બાળક માટે સુરક્ષિત, ટૂંકા અને પુસ્તક આધારિત ગુજરાતી અભ્યાસ મિત્ર છો.",
                prompt = prompt,
            ).cleanLocalModelText()
            if (text.isBlank() || text.contains("પુસ્તકમાં મળ્યું નથી")) groundedFallback else StudyAnswer(
                answerGujarati = text.take(700),
                sourceLabels = request.sources.take(3).map { "${it.bookTitle} • p.${it.pageNumber}" }.distinct(),
                grounded = true,
            )
        }.getOrElse { groundedFallback }
    }

    override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int?): String =
        mock.feedbackGujarati(result, expectedAnswer)

    suspend fun testConnection(): String = if (modelStore.hasModel()) {
        model.test()
    } else {
        "Offline preparation is ready with PDF text extraction and Gujarati/English OCR. Import a .litertlm model for higher-quality chapter concepts and summaries."
    }

    private suspend fun analyzeTocWithModel(
        request: TocAnalysisRequest,
        pages: List<RenderedBookPage>,
    ): TocAnalysisResult {
        val pageText = pages.joinToString("\n\n") { page ->
            "--- PDF PAGE ${page.pageNumber} (${page.extractionMethod.orEmpty()}) ---\n${page.extractedText.orEmpty().take(5_000)}"
        }.take(18_000)
        val prompt = """
            Book: ${request.bookTitle}
            Subject: ${request.subject}
            PDF page count: ${request.pageCount}

            OCR/text from contents or index pages:
            $pageText

            Detect only real chapters shown in the supplied text. Return STRICT JSON only:
            {"chapters":[{"chapterNumber":1,"titleGujarati":"...","titleEnglish":"","startPage":5}]}

            Rules:
            - startPage is a 1-based PDF page number from 1 to ${request.pageCount}.
            - Preserve Gujarati chapter titles.
            - Do not invent chapters.
            - Ignore headings such as contents, index, preface and acknowledgements unless they are actual lessons.
            - If printed book page numbers appear instead of PDF page numbers, make the safest estimate; the parent will review it.
        """.trimIndent()
        val raw = model.generate(
            systemInstruction = "You extract textbook structure from OCR text. Output valid compact JSON only, with no markdown or explanation.",
            prompt = prompt,
        )
        val root = raw.parseFirstJsonObject()
        val chapters = root.array("chapters").mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val title = item.string("titleGujarati").orEmpty().trim()
            val start = item.int("startPage")?.coerceIn(1, request.pageCount) ?: return@mapNotNull null
            if (title.isBlank()) return@mapNotNull null
            TocChapterSuggestion(
                chapterNumber = item.int("chapterNumber"),
                titleGujarati = title,
                titleEnglish = item.string("titleEnglish")?.trim()?.takeIf { it.isNotBlank() },
                startPage = start,
            )
        }.distinctBy { it.startPage }.sortedBy { it.startPage }
        require(chapters.isNotEmpty()) { "The local model returned no usable chapter structure." }
        return TocAnalysisResult(
            chapters = chapters,
            sourceLabel = "Offline OCR/PDF text • LiteRT-LM",
        )
    }

    private fun analyzeTocDeterministically(
        request: TocAnalysisRequest,
        pages: List<RenderedBookPage>,
    ): TocAnalysisResult {
        val candidates = pages.flatMap { page ->
            page.extractedText.orEmpty().lineSequence().mapNotNull { line ->
                parseOfflineTocLine(line, request.pageCount)
            }.toList()
        }.distinctBy { it.startPage }.sortedBy { it.startPage }

        require(candidates.isNotEmpty()) {
            "Offline OCR read the selected pages but could not reliably detect chapter rows. Import a LiteRT-LM model or add chapter ranges manually."
        }
        return TocAnalysisResult(
            chapters = candidates,
            sourceLabel = "Offline PDF text/OCR • rule-based chapter detection",
        )
    }

    private suspend fun analyzeChapterWithModel(
        request: ChapterAnalysisRequest,
        pages: List<RenderedBookPage>,
    ): ChapterAnalysisResult {
        val pageText = pages.joinToString("\n\n") { page ->
            "--- PDF PAGE ${page.pageNumber} (${page.extractionMethod.orEmpty()}) ---\n${page.extractedText.orEmpty().take(3_200)}"
        }.take(16_000)
        val prompt = """
            Prepare local learning metadata for a Standard 2 Gujarati-medium app.
            Book: ${request.bookTitle}
            Subject: ${request.subject}
            Chapter: ${request.chapterTitleGujarati}
            Chapter PDF range: ${request.startPage}-${request.endPage}

            Extracted textbook text:
            $pageText

            Return STRICT JSON only using this shape:
            {
              "pages":[{
                "pageNumber":1,
                "summaryGujarati":"...",
                "importantObjects":["..."],
                "exercises":["..."],
                "concepts":["..."]
              }],
              "concepts":[{
                "titleGujarati":"...",
                "titleEnglish":"",
                "descriptionGujarati":"...",
                "difficulty":1,
                "expectedLearningOutcome":"...",
                "sourcePageStart":1,
                "sourcePageEnd":1,
                "practiceReady":true
              }]
            }

            Rules:
            - Analyze only the supplied extracted text; never invent unsupported facts.
            - Return one page object for every supplied PDF page.
            - If a page has no readable extracted text, say that briefly and do not invent content.
            - Keep Gujarati simple, concise and suitable for Standard 2.
            - Separate independently teachable skills into separate concepts.
            - Page numbers must stay within ${request.startPage}-${request.endPage}.
            - Mark practiceReady false only for administrative, answer-key-only, unsafe or unreadable content.
        """.trimIndent()
        val raw = model.generate(
            systemInstruction = "You convert OCR textbook text into faithful structured learning metadata. Output valid compact JSON only.",
            prompt = prompt,
        )
        val root = raw.parseFirstJsonObject()
        val allowedPages = pages.map { it.pageNumber }.toSet()
        val pageDrafts = root.array("pages").mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val number = item.int("pageNumber") ?: return@mapNotNull null
            if (number !in allowedPages) return@mapNotNull null
            val sourcePage = pages.first { it.pageNumber == number }
            PageKnowledgeDraft(
                pageNumber = number,
                summaryGujarati = item.string("summaryGujarati").orEmpty().ifBlank {
                    summarizeText(sourcePage.extractedText.orEmpty())
                }.take(700),
                // Ground Study Talk in the exact on-device extraction. The model may summarize
                // it, but generated wording must never replace the source textbook text.
                visibleTextGujarati = sourcePage.extractedText.orEmpty().take(6_000),
                importantObjectsJson = item.arrayOrNull("importantObjects")?.takeJsonItems(20),
                exercisesJson = item.arrayOrNull("exercises")?.takeJsonItems(20),
                conceptsJson = item.arrayOrNull("concepts")?.takeJsonItems(20),
            )
        }
        val completedPages = pages.map { source ->
            pageDrafts.firstOrNull { it.pageNumber == source.pageNumber } ?: fallbackPageDraft(source)
        }

        val concepts = root.array("concepts").mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val title = item.string("titleGujarati").orEmpty().trim()
            if (title.isBlank()) return@mapNotNull null
            val start = (item.int("sourcePageStart") ?: pages.first().pageNumber)
                .coerceIn(request.startPage, request.endPage)
            val end = (item.int("sourcePageEnd") ?: start)
                .coerceIn(start, request.endPage)
            ConceptDraft(
                titleGujarati = title,
                titleEnglish = item.string("titleEnglish")?.trim()?.takeIf { it.isNotBlank() },
                descriptionGujarati = item.string("descriptionGujarati").orEmpty().ifBlank {
                    "આ પાઠમાં $title વિશે સમજ અને અભ્યાસ કરવામાં આવે છે."
                },
                difficulty = (item.int("difficulty") ?: 1).coerceIn(1, 5),
                expectedLearningOutcome = item.string("expectedLearningOutcome").orEmpty().ifBlank {
                    "બાળક $title વિશેનો મુખ્ય વિચાર ઓળખી અને સરળ પ્રશ્નનો જવાબ આપી શકે."
                },
                sourcePageStart = start,
                sourcePageEnd = end,
                practiceReady = item.boolean("practiceReady") ?: true,
            )
        }
        val usableConcepts = concepts.ifEmpty { fallbackConcepts(request, pages) }
        return ChapterAnalysisResult(
            pages = completedPages,
            concepts = usableConcepts,
            sourceLabel = "Offline OCR/PDF text • LiteRT-LM",
        )
    }

    private fun analyzeChapterDeterministically(
        request: ChapterAnalysisRequest,
        pages: List<RenderedBookPage>,
    ): ChapterAnalysisResult = ChapterAnalysisResult(
        pages = pages.map(::fallbackPageDraft),
        concepts = fallbackConcepts(request, pages),
        sourceLabel = "Offline PDF text/OCR • local fallback",
    )

    private fun fallbackPageDraft(page: RenderedBookPage): PageKnowledgeDraft {
        val text = page.extractedText.orEmpty().normalizeWhitespace()
        val lines = text.lines().map(String::trim).filter(String::isNotBlank)
        val exercises = lines.filter(::looksLikeExercise).take(12)
        val heading = pickHeading(lines)
        return PageKnowledgeDraft(
            pageNumber = page.pageNumber,
            summaryGujarati = summarizeText(text),
            visibleTextGujarati = text.take(6_000),
            importantObjectsJson = JsonArray(emptyList()).toString(),
            exercisesJson = JsonArray(exercises.map(::JsonPrimitive)).toString(),
            conceptsJson = JsonArray(listOfNotNull(heading?.let(::JsonPrimitive))).toString(),
        )
    }

    private fun fallbackConcepts(
        request: ChapterAnalysisRequest,
        pages: List<RenderedBookPage>,
    ): List<ConceptDraft> {
        val headings = pages.mapNotNull { page ->
            val lines = page.extractedText.orEmpty().lines().map(String::trim).filter(String::isNotBlank)
            pickHeading(lines)?.let { Triple(it, page.pageNumber, page.extractedText.orEmpty()) }
        }.distinctBy { it.first.lowercase() }.take(8)

        val candidates = if (headings.isNotEmpty()) headings else listOf(
            Triple(
                request.chapterTitleGujarati.ifBlank { "પાઠનો મુખ્ય વિચાર" },
                pages.firstOrNull()?.pageNumber ?: request.startPage,
                pages.joinToString(" ") { it.extractedText.orEmpty() },
            )
        )
        return candidates.map { (title, pageNumber, text) ->
            ConceptDraft(
                titleGujarati = title.take(120),
                descriptionGujarati = "આ ભાગમાં ${summarizeText(text).take(220)}",
                difficulty = 1,
                expectedLearningOutcome = "બાળક $title નો મુખ્ય વિચાર ઓળખી અને પુસ્તક આધારિત સરળ જવાબ આપી શકે.",
                sourcePageStart = pageNumber,
                sourcePageEnd = pageNumber,
                practiceReady = text.count { it.isLetterOrDigit() } >= 25 && !looksAdministrative(text),
            )
        }
    }

    private fun pickHeading(lines: List<String>): String? = lines.firstOrNull { line ->
        val compact = line.trim().trim('•', '-', '–', '—', ':')
        compact.length in 3..90 &&
            compact.count(Char::isLetter) >= 2 &&
            !looksLikeExercise(compact) &&
            !looksAdministrative(compact) &&
            !compact.endsWith('.') &&
            !compact.endsWith('?')
    }?.trim()?.take(120)

    private fun looksLikeExercise(text: String): Boolean {
        val value = text.lowercase()
        return EXERCISE_WORDS.any(value::contains) || value.trimStart().matches(Regex("^[0-9]+[.)].+"))
    }

    private fun looksAdministrative(text: String): Boolean {
        val value = text.lowercase()
        return isOfflineAdministrativeText(value)
    }

    private companion object {
        val EXERCISE_WORDS = listOf(
            "પ્રશ્ન", "કસરત", "જવાબ", "લખો", "વાંચો", "બોલો", "ગણો", "જોડો", "ખાલી જગ્યા",
            "question", "exercise", "answer", "write", "read", "count", "fill in",
        )
    }
}



internal fun List<RenderedBookPage>.requireOfflineTextInputs(): List<RenderedBookPage> = also { pages ->
    require(pages.isNotEmpty()) { "Offline preparation received no PDF pages." }
    pages.forEach { page ->
        require(page.extractedText != null) {
            "Offline preparation received an image-only input for PDF page ${page.pageNumber}."
        }
    }
}

private fun summarizeText(source: String): String {
    val clean = source.normalizeWhitespace()
    if (clean.isBlank()) return "આ પાનામાં વાંચી શકાય તેવી માહિતી ઓછી છે."
    val usefulLines = clean.lines().map(String::trim).filter(String::isNotBlank).take(4)
    return usefulLines.joinToString(" ").take(420)
}

private fun String.normalizeWhitespace(): String = replace(Regex("[ \\t]+"), " ")
    .replace(Regex(" *\\n *"), "\n")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()


private fun String.parseFirstJsonObject(): JsonObject {
    val cleaned = cleanLocalModelText()
    val start = cleaned.indexOf('{')
    require(start >= 0) { "The local model did not return JSON." }
    var depth = 0
    var inString = false
    var escaped = false
    for (index in start until cleaned.length) {
        val char = cleaned[index]
        if (inString) {
            if (escaped) escaped = false
            else if (char == '\\') escaped = true
            else if (char == '"') inString = false
        } else {
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return Json.parseToJsonElement(cleaned.substring(start, index + 1)) as JsonObject
                    }
                }
            }
        }
    }
    error("The local model returned incomplete JSON.")
}

private fun JsonObject.array(name: String): JsonArray = this[name] as? JsonArray ?: JsonArray(emptyList())
private fun JsonObject.arrayOrNull(name: String): JsonArray? = this[name] as? JsonArray
private fun JsonArray.takeJsonItems(limit: Int): String = JsonArray(take(limit)).toString()
private fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.int(name: String): Int? = (this[name] as? JsonPrimitive)?.intOrNull
private fun JsonObject.boolean(name: String): Boolean? = (this[name] as? JsonPrimitive)?.booleanOrNull

private fun String.cleanLocalModelText(): String = this
    .replace(Regex("(?s)<think>.*?</think>"), "")
    .replace("```json", "", ignoreCase = true)
    .replace("```", "")
    .trim()
