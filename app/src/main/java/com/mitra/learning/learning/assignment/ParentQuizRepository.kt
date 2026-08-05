package com.mitra.learning.learning.assignment

import android.content.Context
import com.mitra.learning.data.db.dao.ParentQuizDao
import com.mitra.learning.data.db.entity.ParentQuizPlanEntity
import com.mitra.learning.data.db.entity.ParentQuizQuestionEntity
import com.mitra.learning.learning.offline.decodeStringList
import com.mitra.learning.learning.offline.encodeStringList
import com.mitra.learning.study.practice.MitraChallengeEvaluationMode
import com.mitra.learning.study.practice.MitraChallengeKind
import java.io.File
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stores the active parent-created test in Room for fast, queryable access. The legacy private
 * file is retained as a one-release migration fallback so an existing 0.22 test is not lost.
 */
class ParentQuizRepository(
    context: Context,
    private val dao: ParentQuizDao? = null,
) {
    private val file = File(context.filesDir, "parent_quiz/active.quiz")
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    suspend fun save(plan: ParentQuizPlan) = withContext(Dispatchers.IO) {
        dao?.replace(
            plan = plan.toEntity(),
            questions = plan.questions.mapIndexed { index, question -> question.toEntity(plan.id, index) },
        )
        // Keep a compatible fallback for installs whose database is temporarily unavailable.
        saveLegacy(plan)
    }

    suspend fun load(): ParentQuizPlan? = withContext(Dispatchers.IO) {
        val roomDao = dao
        val roomPlan = roomDao?.activePlan()
        if (roomPlan != null) {
            return@withContext roomPlan.toModel(roomDao.questions(roomPlan.id))
        }
        val legacy = loadLegacy() ?: return@withContext null
        dao?.replace(
            plan = legacy.toEntity(),
            questions = legacy.questions.mapIndexed { index, question -> question.toEntity(legacy.id, index) },
        )
        legacy
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dao?.clear()
        file.delete()
    }

    suspend fun exists(): Boolean = withContext(Dispatchers.IO) {
        dao?.activePlan() != null || (file.isFile && file.length() > 0)
    }

    private fun ParentQuizPlan.toEntity() = ParentQuizPlanEntity(
        id = id,
        title = title,
        topic = topic.name,
        createdAt = createdAt,
        skillConceptId = skillConceptId,
        skillTitleGujarati = skillTitleGujarati,
        bookId = bookId,
        bookTitle = bookTitle,
        chapterId = chapterId,
        chapterTitleGujarati = chapterTitleGujarati,
    )

    private fun ParentQuizQuestion.toEntity(planId: String, position: Int) = ParentQuizQuestionEntity(
        id = "$planId:$position:$id",
        planId = planId,
        position = position,
        promptGujarati = promptGujarati,
        spokenPrompt = spokenPrompt,
        recognitionLanguageTag = recognitionLanguageTag,
        kind = kind.name,
        evaluationMode = evaluationMode.name,
        expectedNumber = expectedNumber,
        expectedText = expectedText,
        acceptedAnswersJson = encodeStringList(acceptedAnswers),
        hintGujarati = hintGujarati,
        correctionGujarati = correctionGujarati,
        sourceLabelsJson = encodeStringList(sourceLabels),
    )

    private fun ParentQuizPlanEntity.toModel(questions: List<ParentQuizQuestionEntity>) = ParentQuizPlan(
        id = id,
        title = title,
        topic = ParentQuizTopic.valueOf(topic),
        createdAt = createdAt,
        questions = questions.map { it.toModel() },
        skillConceptId = skillConceptId,
        skillTitleGujarati = skillTitleGujarati,
        bookId = bookId,
        bookTitle = bookTitle,
        chapterId = chapterId,
        chapterTitleGujarati = chapterTitleGujarati,
    )

    private fun ParentQuizQuestionEntity.toModel() = ParentQuizQuestion(
        id = id,
        promptGujarati = promptGujarati,
        spokenPrompt = spokenPrompt,
        recognitionLanguageTag = recognitionLanguageTag,
        kind = MitraChallengeKind.valueOf(kind),
        evaluationMode = MitraChallengeEvaluationMode.valueOf(evaluationMode),
        expectedNumber = expectedNumber,
        expectedText = expectedText,
        acceptedAnswers = decodeStringList(acceptedAnswersJson),
        hintGujarati = hintGujarati,
        correctionGujarati = correctionGujarati,
        sourceLabels = decodeStringList(sourceLabelsJson),
    )

    private fun saveLegacy(plan: ParentQuizPlan) {
        file.parentFile?.mkdirs()
        val lines = buildList {
            add(listOf(
                enc(plan.id), enc(plan.title), plan.topic.name, plan.createdAt.toString(),
                enc(plan.skillConceptId.orEmpty()), enc(plan.skillTitleGujarati.orEmpty()),
                enc(plan.bookId.orEmpty()), enc(plan.bookTitle.orEmpty()),
                enc(plan.chapterId.orEmpty()), enc(plan.chapterTitleGujarati.orEmpty()),
            ).joinToString("|"))
            plan.questions.forEach { q ->
                add(listOf(
                    enc(q.id), enc(q.promptGujarati), enc(q.spokenPrompt), q.recognitionLanguageTag,
                    q.kind.name, q.evaluationMode.name, q.expectedNumber?.toString().orEmpty(),
                    enc(q.expectedText.orEmpty()), enc(q.acceptedAnswers.joinToString("\u001F")),
                    enc(q.hintGujarati.orEmpty()), enc(q.correctionGujarati), enc(q.sourceLabels.joinToString("\u001F")),
                ).joinToString("|"))
            }
        }
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(lines.joinToString("\n"))
        check(temporary.renameTo(file) || run { temporary.copyTo(file, overwrite = true); temporary.delete(); true })
    }

    private fun loadLegacy(): ParentQuizPlan? {
        if (!file.isFile) return null
        return runCatching {
            val lines = file.readLines().filter(String::isNotBlank)
            val header = lines.first().split('|')
            val questions = lines.drop(1).map { line ->
                val f = line.split('|')
                ParentQuizQuestion(
                    id = dec(f[0]), promptGujarati = dec(f[1]), spokenPrompt = dec(f[2]),
                    recognitionLanguageTag = f[3], kind = MitraChallengeKind.valueOf(f[4]),
                    evaluationMode = MitraChallengeEvaluationMode.valueOf(f[5]),
                    expectedNumber = f[6].toIntOrNull(), expectedText = dec(f[7]).ifBlank { null },
                    acceptedAnswers = dec(f[8]).split('\u001F').filter(String::isNotBlank),
                    hintGujarati = dec(f[9]).ifBlank { null }, correctionGujarati = dec(f[10]),
                    sourceLabels = dec(f[11]).split('\u001F').filter(String::isNotBlank),
                )
            }
            ParentQuizPlan(
                id = dec(header[0]),
                title = dec(header[1]),
                topic = ParentQuizTopic.valueOf(header[2]),
                createdAt = header[3].toLong(),
                questions = questions,
                skillConceptId = header.getOrNull(4)?.let(::dec)?.ifBlank { null },
                skillTitleGujarati = header.getOrNull(5)?.let(::dec)?.ifBlank { null },
                bookId = header.getOrNull(6)?.let(::dec)?.ifBlank { null },
                bookTitle = header.getOrNull(7)?.let(::dec)?.ifBlank { null },
                chapterId = header.getOrNull(8)?.let(::dec)?.ifBlank { null },
                chapterTitleGujarati = header.getOrNull(9)?.let(::dec)?.ifBlank { null },
            )
        }.getOrNull()
    }

    private fun enc(value: String): String = encoder.encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun dec(value: String): String = String(decoder.decode(value), Charsets.UTF_8)
}
