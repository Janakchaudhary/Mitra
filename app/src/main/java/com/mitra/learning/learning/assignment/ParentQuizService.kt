package com.mitra.learning.learning.assignment

import com.mitra.learning.learning.curriculum.BuiltInCurriculum
import com.mitra.learning.learning.curriculum.Standard2SkillActivityFactory
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.study.practice.MitraChallengeEvaluationMode
import com.mitra.learning.study.practice.MitraChallengeKind
import com.mitra.learning.study.practice.MitraPracticeTopic
import com.mitra.learning.study.practice.MitraVoicePracticeService
import java.util.UUID

class ParentQuizService(
    private val practiceService: MitraVoicePracticeService,
    private val repository: ParentQuizRepository,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun create(
        title: String,
        topic: ParentQuizTopic,
        count: Int,
        skillConceptId: String? = null,
    ): ParentQuizPlan {
        val requested = count.coerceIn(5, 25)
        if (topic == ParentQuizTopic.SKILL) {
            return createSkillPlan(title, requested, skillConceptId)
        }

        val practiceTopic = when (topic) {
            ParentQuizTopic.SKILL -> error("Skill topic is handled separately")
            ParentQuizTopic.PREPARED_BOOK -> MitraPracticeTopic.BOOK
            ParentQuizTopic.TABLES -> MitraPracticeTopic.TABLES
            ParentQuizTopic.NUMBER_NEIGHBORS -> MitraPracticeTopic.NUMBER_NEIGHBORS
            ParentQuizTopic.SPELLING -> MitraPracticeTopic.SPELLING
            ParentQuizTopic.MIXED -> MitraPracticeTopic.MIXED
        }
        val questions = mutableListOf<ParentQuizQuestion>()
        val seen = mutableSetOf<String>()
        var previous: String? = null
        var attempts = 0
        while (questions.size < requested && attempts < requested * 8) {
            attempts += 1
            val challenge = practiceService.nextChallenge(practiceTopic, previous)
            if (challenge.id == "book-unavailable") {
                if (topic == ParentQuizTopic.PREPARED_BOOK && questions.isEmpty()) {
                    error("તૈયાર પુસ્તકમાંથી પ્રશ્ન બનાવવા પહેલાં ઓછામાં ઓછો એક પાઠ Prepare કરો.")
                }
                if (topic == ParentQuizTopic.MIXED) {
                    previous = null
                    continue
                }
                break
            }
            previous = challenge.id
            if (!seen.add(challenge.id) && attempts < requested * 5) continue
            questions += ParentQuizQuestion(
                id = "${challenge.id}-${questions.size}",
                promptGujarati = challenge.promptGujarati,
                spokenPrompt = challenge.spokenPrompt,
                recognitionLanguageTag = challenge.recognitionLanguageTag,
                kind = challenge.kind,
                evaluationMode = challenge.evaluationMode,
                expectedNumber = challenge.expectedNumber,
                expectedText = challenge.expectedText,
                acceptedAnswers = challenge.acceptedAnswers,
                hintGujarati = challenge.hintGujarati,
                correctionGujarati = challenge.correctionGujarati,
                sourceLabels = challenge.sourceLabels,
            )
        }
        require(questions.isNotEmpty()) { "પ્રશ્ન તૈયાર થઈ શક્યા નહીં." }
        val plan = ParentQuizPlan(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "મિત્ર કસોટી" },
            topic = topic,
            createdAt = now(),
            questions = questions,
        )
        repository.save(plan)
        return plan
    }

    private suspend fun createSkillPlan(
        title: String,
        requested: Int,
        skillConceptId: String?,
    ): ParentQuizPlan {
        val concept = BuiltInCurriculum.concepts.firstOrNull { it.id == skillConceptId }
            ?: error("કસોટી બનાવવા માટે એક કૌશલ્ય પસંદ કરો.")

        val unique = linkedMapOf<String, LearningQuestion>()
        repeat(10) { pass ->
            if (unique.size >= requested) return@repeat
            Standard2SkillActivityFactory.create(
                concept = concept,
                count = 25,
                seed = now() + pass * 9_973L,
            ).forEach { question -> unique.putIfAbsent(question.fingerprint, question) }
        }
        val base = unique.values.toList()
        require(base.isNotEmpty()) { "આ કૌશલ્ય માટે પ્રશ્ન તૈયાર થઈ શક્યા નહીં." }

        // Small language banks contain fewer than 20 distinct prompts. Repeat them only after
        // every unique prompt has appeared, while keeping every test item independently marked.
        val expanded = buildList {
            while (size < requested) {
                base.forEach { question ->
                    if (size < requested) add(question)
                }
            }
        }
        val questions = expanded.take(requested).mapIndexed { index, question ->
            question.toParentQuizQuestion(index, concept.titleGujarati)
        }
        val plan = ParentQuizPlan(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "${concept.titleGujarati} કસોટી" },
            topic = ParentQuizTopic.SKILL,
            createdAt = now(),
            questions = questions,
            skillConceptId = concept.id,
            skillTitleGujarati = concept.titleGujarati,
        )
        repository.save(plan)
        return plan
    }

    private fun LearningQuestion.toParentQuizQuestion(index: Int, skillTitle: String): ParentQuizQuestion {
        val mode = when (evaluationMode) {
            EvaluationMode.NUMERIC -> MitraChallengeEvaluationMode.NUMERIC
            EvaluationMode.KEYWORD -> MitraChallengeEvaluationMode.KEYWORD
            EvaluationMode.MULTIPLE_CHOICE,
            EvaluationMode.SHORT_TEXT,
            EvaluationMode.PARTICIPATION,
            -> MitraChallengeEvaluationMode.EXACT_TEXT
        }
        val challengeKind = when {
            type == ActivityType.SPELLING -> MitraChallengeKind.SPELLING
            type == ActivityType.TABLES -> MitraChallengeKind.TABLE
            else -> MitraChallengeKind.BOOK
        }
        val textAnswer = expectedText?.takeIf(String::isNotBlank)
        val accepted = buildSet {
            textAnswer?.let(::add)
            acceptedAnswers.filter(String::isNotBlank).forEach(::add)
        }.toList()
        val correction = when {
            expectedAnswer != null -> "સાચો જવાબ ${expectedAnswer} છે. ${hintGujarati.orEmpty()}".trim()
            textAnswer != null -> "સાચો જવાબ ‘$textAnswer’ છે. ${hintGujarati.orEmpty()}".trim()
            else -> hintGujarati ?: "પ્રશ્ન ફરી વાંચીને યોગ્ય જવાબ આપો."
        }
        return ParentQuizQuestion(
            id = "skill-${id}-$index",
            promptGujarati = promptGujarati,
            spokenPrompt = speechTextGujarati,
            recognitionLanguageTag = recognitionLanguageTag?.takeIf(String::isNotBlank) ?: "gu-IN",
            kind = challengeKind,
            evaluationMode = mode,
            expectedNumber = expectedAnswer,
            expectedText = textAnswer,
            acceptedAnswers = accepted,
            hintGujarati = hintGujarati,
            correctionGujarati = correction,
            sourceLabels = listOf("કૌશલ્ય: $skillTitle"),
        )
    }
}
