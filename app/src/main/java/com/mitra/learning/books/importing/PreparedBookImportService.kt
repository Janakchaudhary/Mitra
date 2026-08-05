package com.mitra.learning.books.importing

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.mitra.learning.data.db.MitraDatabase
import com.mitra.learning.data.db.entity.BookAnalysisStatus
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.PageKnowledgeEntity
import com.mitra.learning.data.db.entity.PageKnowledgeFtsEntity
import com.mitra.learning.data.db.entity.VocabularyEntity
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.learning.offline.OfflineQuestionBank
import com.mitra.learning.learning.offline.encodeStringList
import com.mitra.learning.learning.offline.toPreparedQuestionEntity
import com.mitra.learning.study.BookTextNormalizer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface PreparedBookImportResult {
    data class Success(
        val book: BookEntity,
        val attachedToExistingPdf: Boolean,
        val chapterCount: Int,
        val questionCount: Int,
    ) : PreparedBookImportResult

    data class Failure(val message: String) : PreparedBookImportResult
}

class PreparedBookImportService(
    context: Context,
    private val database: MitraDatabase,
    private val questionBank: OfflineQuestionBank,
) {
    private val appContext = context.applicationContext

    suspend fun import(source: Uri): PreparedBookImportResult = withContext(Dispatchers.IO) {
        runCatching {
            val packageBytes = appContext.contentResolver.openInputStream(source)?.use(::readLimited)
                ?: error("Could not open the selected prepared-book file")
            val jsonText = extractJson(packageBytes)
            val prepared = PreparedBookPackageParser.parse(jsonText)
            importValidated(prepared, packageBytes)
        }.getOrElse { error ->
            PreparedBookImportResult.Failure(
                error.message?.takeIf { it.isNotBlank() } ?: "Prepared-book import failed"
            )
        }
    }

    private suspend fun importValidated(
        prepared: PreparedBookPackage,
        packageBytes: ByteArray,
    ): PreparedBookImportResult.Success {
        val packageDigest = sha256(packageBytes)
        val sourceSha = prepared.book.sourcePdfSha256
        val existingByHash = sourceSha?.let { database.bookDao().findBySha256(it) }
        val existingByMetadata = if (existingByHash == null && sourceSha == null) {
            database.bookDao().getAll().filter { candidate ->
                candidate.title.trim().equals(prepared.book.title.trim(), ignoreCase = true) &&
                    candidate.subject.trim().equals(prepared.book.subject.trim(), ignoreCase = true) &&
                    candidate.standard == prepared.book.standard &&
                    candidate.pageCount == prepared.book.pageCount
            }.singleOrNull()
        } else {
            null
        }
        val existing = existingByHash ?: existingByMetadata
        val matchingSha = existing?.sha256 ?: sourceSha ?: packageDigest
        val bookId = existing?.id ?: UUID.randomUUID().toString()
        val bookDirectory = File(appContext.filesDir, "books/$bookId").apply { mkdirs() }
        val packageFile = File(bookDirectory, "prepared-book.mitrabook")
        val temporaryPackageFile = File(bookDirectory, "prepared-book.mitrabook.tmp")
        temporaryPackageFile.writeBytes(packageBytes)

        val oldConceptIds = if (existing != null) {
            database.conceptDao().getAll().asSequence()
                .filter { it.bookId == existing.id }
                .map { it.id }
                .toList()
        } else {
            emptyList()
        }

        val chapterIds = prepared.chapters.associate { it.key to UUID.randomUUID().toString() }
        val conceptIds = linkedMapOf<String, String>()
        prepared.chapters.forEach { chapter ->
            chapter.concepts.forEach { concept ->
                val qualifiedKey = "${chapter.key}/${concept.key}"
                require(qualifiedKey !in conceptIds) { "Duplicate concept key: ${concept.key}" }
                conceptIds[qualifiedKey] = UUID.randomUUID().toString()
            }
            if (chapter.vocabulary.isNotEmpty()) {
                conceptIds["${chapter.key}/__vocabulary__"] = UUID.randomUUID().toString()
            }
        }

        val chapterEntities = prepared.chapters.map { chapter ->
            ChapterEntity(
                id = chapterIds.getValue(chapter.key),
                bookId = bookId,
                chapterNumber = chapter.chapterNumber,
                titleGujarati = chapter.titleGujarati,
                titleEnglish = chapter.titleEnglish,
                startPage = chapter.startPage,
                endPage = chapter.endPage,
                analysisStatus = ChapterAnalysisStatus.READY,
            )
        }

        val conceptEntities = mutableListOf<ConceptEntity>()
        val questionsByConcept = linkedMapOf<String, MutableList<LearningQuestion>>()
        prepared.chapters.forEachIndexed { chapterIndex, chapter ->
            val chapterId = chapterIds.getValue(chapter.key)
            chapter.concepts.forEachIndexed { conceptIndex, concept ->
                val conceptId = conceptIds.getValue("${chapter.key}/${concept.key}")
                conceptEntities += ConceptEntity(
                    id = conceptId,
                    subject = prepared.book.subject,
                    standard = prepared.book.standard,
                    language = prepared.book.language,
                    titleGujarati = concept.titleGujarati,
                    titleEnglish = concept.titleEnglish,
                    descriptionGujarati = concept.descriptionGujarati,
                    difficulty = concept.difficulty,
                    expectedLearningOutcome = concept.expectedLearningOutcome,
                    sortOrder = (chapterIndex + 1) * 10_000 + conceptIndex,
                    builtIn = false,
                    bookId = bookId,
                    chapterId = chapterId,
                    sourcePageStart = concept.sourcePageStart,
                    sourcePageEnd = concept.sourcePageEnd,
                    practiceReady = concept.practiceReady,
                )
                questionsByConcept.getOrPut(conceptId) { mutableListOf() }.addAll(
                    concept.questions.map { question ->
                        question.copy(
                            id = uniqueQuestionId(question.id),
                            conceptId = conceptId,
                            sourcePage = question.sourcePage ?: concept.sourcePageStart,
                        )
                    }
                )
            }

            if (chapter.vocabulary.isNotEmpty()) {
                val conceptId = conceptIds.getValue("${chapter.key}/__vocabulary__")
                conceptEntities += ConceptEntity(
                    id = conceptId,
                    subject = prepared.book.subject,
                    standard = prepared.book.standard,
                    language = prepared.book.language,
                    titleGujarati = "શબ્દભંડોળ – ${chapter.titleGujarati}",
                    titleEnglish = "Vocabulary",
                    descriptionGujarati = "આ પાઠના મહત્વના શબ્દોના સરળ અર્થ અને વાક્યપ્રયોગ.",
                    difficulty = 1,
                    expectedLearningOutcome = "બાળક પાઠના મહત્વના શબ્દોના અર્થ કહી અને વાક્યમાં વાપરી શકે.",
                    sortOrder = (chapterIndex + 1) * 10_000 + 9_000,
                    builtIn = false,
                    bookId = bookId,
                    chapterId = chapterId,
                    sourcePageStart = chapter.startPage,
                    sourcePageEnd = chapter.endPage,
                    practiceReady = true,
                )
                questionsByConcept.getOrPut(conceptId) { mutableListOf() }.addAll(
                    chapter.vocabulary.map { word -> vocabularyQuestion(word, conceptId) }
                )
            }
        }

        val pageEntities = buildPageKnowledge(prepared, bookId, chapterIds)
        val duplicatePages = pageEntities.groupingBy { it.pageNumber }.eachCount().filterValues { it > 1 }.keys
        require(duplicatePages.isEmpty()) {
            "Physical PDF pages cannot belong to multiple chapters: ${duplicatePages.sorted().joinToString()}"
        }

        val vocabularyEntities = prepared.chapters.flatMap { chapter ->
            val chapterId = chapterIds.getValue(chapter.key)
            chapter.vocabulary.map { word ->
                VocabularyEntity(
                    id = "vocabulary:$bookId:$chapterId:${BookTextNormalizer.normalizeWord(word.word)}",
                    bookId = bookId,
                    chapterId = chapterId,
                    word = word.word.trim(),
                    normalizedWord = BookTextNormalizer.normalizeWord(word.word),
                    meaningGujarati = word.meaningGujarati.trim(),
                    simpleExplanationGujarati = word.simpleExplanationGujarati?.trim()?.takeIf(String::isNotBlank),
                    exampleSentenceGujarati = word.exampleSentenceGujarati?.trim()?.takeIf(String::isNotBlank),
                    sourcePage = word.sourcePage,
                    acceptedVoiceFormsJson = encodeStringList(word.acceptedVoiceForms),
                )
            }
        }

        val conceptById = conceptEntities.associateBy { it.id }
        val preparedQuestionEntities = questionsByConcept.flatMap { (conceptId, questions) ->
            val concept = conceptById.getValue(conceptId)
            questions.distinctBy { it.fingerprint }.take(250).map { question ->
                question.toPreparedQuestionEntity(
                    bookId = bookId,
                    chapterId = requireNotNull(concept.chapterId),
                    conceptIdOverride = conceptId,
                    idOverride = question.id,
                    difficulty = concept.difficulty,
                )
            }
        }

        val book = if (existing != null) {
            existing.copy(
                title = prepared.book.title,
                subject = prepared.book.subject,
                standard = prepared.book.standard,
                language = prepared.book.language,
                pageCount = maxOf(existing.pageCount, prepared.book.pageCount),
                analysisStatus = BookAnalysisStatus.READY,
            )
        } else {
            BookEntity(
                id = bookId,
                title = prepared.book.title,
                subject = prepared.book.subject,
                standard = prepared.book.standard,
                language = prepared.book.language,
                localPdfPath = "",
                sha256 = matchingSha,
                pageCount = prepared.book.pageCount,
                coverPath = null,
                createdAt = System.currentTimeMillis(),
                analysisStatus = BookAnalysisStatus.READY,
            )
        }

        database.withTransaction {
            if (existing == null) {
                database.bookDao().insert(book)
            } else {
                database.pageKnowledgeFtsDao().deleteForBook(bookId)
                database.pageKnowledgeDao().deleteForBook(bookId)
                database.vocabularyDao().deleteForBook(bookId)
                database.preparedQuestionDao().deleteForBook(bookId)
                database.conceptDao().deleteForBook(bookId)
                database.chapterDao().deleteForBook(bookId)
                database.bookDao().update(book)
            }
            database.chapterDao().upsertAll(chapterEntities)
            database.pageKnowledgeDao().upsertAll(pageEntities)
            database.pageKnowledgeFtsDao().upsertAll(pageEntities.map(::pageToFts))
            database.conceptDao().upsertAll(conceptEntities)
            database.vocabularyDao().upsertAll(vocabularyEntities)
            database.preparedQuestionDao().upsertAll(preparedQuestionEntities)
        }

        oldConceptIds.forEach(questionBank::deleteForConcept)
        var questionCount = preparedQuestionEntities.size
        questionsByConcept.forEach { (conceptId, questions) ->
            val clean = questions.distinctBy { it.fingerprint }.take(250)
            if (clean.isNotEmpty()) {
                questionBank.save(conceptId, clean)
            }
        }
        if (packageFile.exists()) packageFile.delete()
        if (!temporaryPackageFile.renameTo(packageFile)) {
            temporaryPackageFile.copyTo(packageFile, overwrite = true)
            temporaryPackageFile.delete()
        }

        return PreparedBookImportResult.Success(
            book = book,
            attachedToExistingPdf = existing?.localPdfPath?.isNotBlank() == true,
            chapterCount = chapterEntities.size,
            questionCount = questionCount,
        )
    }

    private fun buildPageKnowledge(
        prepared: PreparedBookPackage,
        bookId: String,
        chapterIds: Map<String, String>,
    ): List<PageKnowledgeEntity> {
        val now = System.currentTimeMillis()
        return prepared.chapters.flatMap { chapter ->
            val pages = chapter.pages.associateBy { it.pageNumber }.toMutableMap()
            val vocabByPage = chapter.vocabulary.groupBy { it.sourcePage }
            (pages.keys + vocabByPage.keys + chapter.startPage).distinct().sorted().map { pageNumber ->
                val source = pages[pageNumber]
                val vocabulary = vocabByPage[pageNumber].orEmpty()
                val vocabularyText = vocabulary.joinToString("\n") { word ->
                    buildString {
                        append("શબ્દ: ${word.word}\nઅર્થ: ${word.meaningGujarati}")
                        word.simpleExplanationGujarati?.takeIf { it.isNotBlank() }?.let { append("\nસરળ સમજ: $it") }
                        word.exampleSentenceGujarati?.takeIf { it.isNotBlank() }?.let { append("\nઉદાહરણ: $it") }
                        if (word.acceptedVoiceForms.isNotEmpty()) {
                            append("\nશોધ સ્વરૂપ: ${word.acceptedVoiceForms.joinToString(", ")}")
                        }
                    }
                }
                val visible = listOfNotNull(source?.visibleTextGujarati, vocabularyText.takeIf { it.isNotBlank() })
                    .joinToString("\n\n")
                    .takeIf { it.isNotBlank() }
                val vocabularySummary = vocabulary.joinToString("; ") { "${it.word} એટલે ${it.meaningGujarati}" }
                val summary = listOfNotNull(
                    source?.summaryGujarati,
                    vocabularySummary.takeIf { it.isNotBlank() },
                    chapter.summaryGujarati.takeIf { pageNumber == chapter.startPage },
                ).joinToString("\n").ifBlank { "${chapter.titleGujarati} પાઠનું તૈયાર જ્ઞાન" }

                PageKnowledgeEntity(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    chapterId = chapterIds.getValue(chapter.key),
                    pageNumber = pageNumber,
                    summaryGujarati = summary,
                    visibleTextGujarati = visible,
                    importantObjectsJson = source?.importantObjectsJson,
                    exercisesJson = source?.exercisesJson,
                    conceptsJson = source?.conceptsJson,
                    analyzedAt = now,
                )
            }
        }
    }


    private fun pageToFts(page: PageKnowledgeEntity): PageKnowledgeFtsEntity = PageKnowledgeFtsEntity(
        rowId = stableRowId(page.id),
        pageKnowledgeId = page.id,
        bookId = page.bookId,
        chapterId = page.chapterId,
        pageNumberText = page.pageNumber.toString(),
        content = listOfNotNull(
            page.summaryGujarati,
            page.visibleTextGujarati,
            page.importantObjectsJson,
            page.exercisesJson,
            page.conceptsJson,
        ).joinToString("\n"),
    )

    private fun stableRowId(value: String): Long {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return ByteBuffer.wrap(bytes.copyOfRange(0, 8)).long and Long.MAX_VALUE
    }

    private fun vocabularyQuestion(word: PreparedVocabulary, conceptId: String): LearningQuestion {
        val accepted = buildList {
            add(word.meaningGujarati)
            word.simpleExplanationGujarati?.takeIf { it.isNotBlank() }?.let(::add)
            addAll(meaningKeywords(word.meaningGujarati))
        }.distinct()
        return LearningQuestion(
            id = "vocabulary-${UUID.randomUUID()}",
            promptGujarati = "‘${word.word}’ નો અર્થ શું?",
            spokenPromptGujarati = "${word.word} શબ્દનો અર્થ શું?",
            speechLanguageTag = "gu-IN",
            recognitionLanguageTag = "gu-IN",
            activityType = ActivityType.VOCABULARY.name,
            evaluationMode = EvaluationMode.KEYWORD,
            expectedText = word.meaningGujarati,
            acceptedAnswers = accepted,
            hintGujarati = word.simpleExplanationGujarati ?: "પાઠમાં આ શબ્દ જ્યાં આવ્યો છે તે વાક્ય યાદ કરો.",
            sourcePage = word.sourcePage,
            conceptId = conceptId,
        )
    }

    private fun meaningKeywords(text: String): List<String> {
        val ignored = setOf("અને", "માટે", "થાય", "એટલે", "એક", "આ", "તે", "જે", "નો", "ની", "નું", "ને", "થી")
        return text
            .replace(Regex("[\\p{Punct}।॥]+"), " ")
            .split(Regex("\\s+"))
            .map(String::trim)
            .filter { it.length >= 3 && it !in ignored }
            .distinct()
            .take(5)
    }

    private fun uniqueQuestionId(input: String): String =
        "import-${input.take(50).replace(Regex("[^A-Za-z0-9._-]"), "_")}-${UUID.randomUUID()}"

    private fun readLimited(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            require(total <= MAX_PACKAGE_BYTES) { "Prepared-book file is larger than 25 MB" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun extractJson(bytes: ByteArray): String {
        val isZip = bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        val text = if (!isZip) {
            bytes.toString(Charsets.UTF_8)
        } else {
            var selected: ByteArray? = null
            var fallback: ByteArray? = null
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && (entry.name.endsWith(".json", true) || entry.name.endsWith(".mitrabook", true))) {
                        val content = readLimited(zip)
                        if (entry.name.substringAfterLast('/').equals("book.json", true) ||
                            entry.name.substringAfterLast('/').equals("prepared-book.json", true)
                        ) {
                            selected = content
                            break
                        }
                        if (fallback == null) fallback = content
                    }
                    zip.closeEntry()
                }
            }
            (selected ?: fallback ?: error("ZIP does not contain a prepared-book JSON file")).toString(Charsets.UTF_8)
        }
        return text.removePrefix("\uFEFF").trim()
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_PACKAGE_BYTES = 25 * 1024 * 1024
    }
}
