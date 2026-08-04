package com.mitra.learning.learning.assignment

import android.content.Context
import com.mitra.learning.study.practice.MitraChallengeEvaluationMode
import com.mitra.learning.study.practice.MitraChallengeKind
import java.io.File
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ParentQuizRepository(context: Context) {
    private val file = File(context.filesDir, "parent_quiz/active.quiz")
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    suspend fun save(plan: ParentQuizPlan) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        val lines = buildList {
            add(listOf(
                enc(plan.id), enc(plan.title), plan.topic.name, plan.createdAt.toString(),
                enc(plan.skillConceptId.orEmpty()), enc(plan.skillTitleGujarati.orEmpty()),
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

    suspend fun load(): ParentQuizPlan? = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext null
        runCatching {
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
            )
        }.getOrNull()
    }

    suspend fun clear() = withContext(Dispatchers.IO) { file.delete() }
    suspend fun exists(): Boolean = withContext(Dispatchers.IO) { file.isFile && file.length() > 0 }

    private fun enc(value: String): String = encoder.encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun dec(value: String): String = String(decoder.decode(value), Charsets.UTF_8)
}
