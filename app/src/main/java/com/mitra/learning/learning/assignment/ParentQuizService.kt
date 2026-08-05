package com.mitra.learning.learning.assignment

import com.mitra.learning.data.db.dao.BookDao
import com.mitra.learning.data.db.dao.ChapterDao
import com.mitra.learning.data.db.dao.ConceptDao
import com.mitra.learning.data.db.dao.PreparedQuestionDao
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.learning.curriculum.BuiltInCurriculum
import com.mitra.learning.learning.curriculum.Standard2SkillActivityFactory
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.learning.offline.OfflineQuestionBank
import com.mitra.learning.learning.offline.toLearningQuestion
import com.mitra.learning.study.practice.MitraChallengeEvaluationMode
import com.mitra.learning.study.practice.MitraChallengeKind
import com.mitra.learning.study.practice.MitraPracticeTopic
import com.mitra.learning.study.practice.MitraVoicePracticeService
import java.util.UUID

class ParentQuizService(
    private val practiceService: MitraVoicePracticeService,
    private val repository: ParentQuizRepository,
    private val bookDao: BookDao? = null,
    private val chapterDao: ChapterDao? = null,
    private val conceptDao: ConceptDao? = null,
    private val preparedQuestionDao: PreparedQuestionDao? = null,
    private val questionBank: OfflineQuestionBank? = null,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun create(
        title: String,
        topic: ParentQuizTopic,
        count: Int,
        skillConceptId: String? = null,
        bookId: String? = null,
        chapterId: String? = null,
    ): ParentQuizPlan {
        val requested = count.coerceIn(5, 25)
        if (topic == ParentQuizTopic.SKILL) return createSkillPlan(title, requested, skillConceptId)
        if (topic == ParentQuizTopic.PREPARED_BOOK) {
            return createPreparedChapterPlan(title, requested, bookId, chapterId)
        }

        val practiceTopic = when (topic) {
            ParentQuizTopic.SKILL, ParentQuizTopic.PREPARED_BOOK -> error("Handled separately")
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
            previous = challenge.id
            if (!seen.add(challenge.id) && attempts < requested * 5) continue
            questions += challenge.toParentQuestion(questions.size)
        }
        require(questions.isNotEmpty()) { "પ્રશ્ન તૈયાર થઈ શક્યા નહીં." }
        return savePlan(
            ParentQuizPlan(
                id = UUID.randomUUID().toString(),
                title = title.trim().ifBlank { "મિત્ર કસોટી" },
                topic = topic,
                createdAt = now(),
                questions = questions,
            )
        )
    }

    private suspend fun createPreparedChapterPlan(
        title: String,
        requested: Int,
        requestedBookId: String?,
        requestedChapterId: String?,
    ): ParentQuizPlan {
        val selectedBookId = requestedBookId?.takeIf(String::isNotBlank)
            ?: error("પહેલાં પુસ્તક પસંદ કરો.")
        val selectedChapterId = requestedChapterId?.takeIf(String::isNotBlank)
            ?: error("પહેલાં પાઠ પસંદ કરો.")
        val book = bookDao?.findById(selectedBookId) ?: error("પસંદ કરેલું પુસ્તક મળ્યું નહીં.")
        val chapter = chapterDao?.findById(selectedChapterId) ?: error("પસંદ કરેલો પાઠ મળ્યો નહીં.")
        require(chapter.bookId == book.id) { "આ પાઠ પસંદ કરેલા પુસ્તકનો નથી." }
        require(chapter.analysisStatus == ChapterAnalysisStatus.READY) { "આ પાઠ પહેલાં Prepare કરો." }

        val roomEntities = preparedQuestionDao?.forChapter(chapter.id, 250).orEmpty()
        val roomQuestions = roomEntities.map { it.toLearningQuestion() }
        val fileQuestions = if (roomQuestions.isEmpty()) {
            conceptDao?.forChapter(chapter.id).orEmpty()
                .filter { it.practiceReady }
                .flatMap { concept -> questionBank?.load(concept.id, 100).orEmpty() }
        } else emptyList()
        val unique = (roomQuestions + fileQuestions)
            .filter(::isAssessable)
            .distinctBy { it.fingerprint }
        require(unique.isNotEmpty()) {
            "આ પાઠ માટે પ્રશ્નો મળ્યા નહીં. પાઠ ફરી Prepare કરો અથવા ChatGPT તૈયાર પુસ્તક Import કરો."
        }
        require(unique.size >= requested) {
            "આ પાઠમાં ${unique.size} અલગ પ્રશ્ન છે. $requested ગુણની કસોટી માટે પાઠ ફરી Prepare કરો અથવા વધુ પ્રશ્નો ધરાવતી .mitrabook file Import કરો."
        }

        val selectedQuestions = FocusedChapterQuestionSelector.select(unique, requested)
        val source = "${book.title} • ${chapter.titleGujarati}"
        val questions = selectedQuestions.mapIndexed { index, question ->
            question.toParentQuizQuestion(index, source)
        }
        val roomIds = roomEntities.mapTo(hashSetOf()) { it.id }
        val usedRoomIds = selectedQuestions.map { it.id }.filter { it in roomIds }
        if (usedRoomIds.isNotEmpty()) {
            preparedQuestionDao?.markUsed(usedRoomIds, now())
        }
        return savePlan(
            ParentQuizPlan(
                id = UUID.randomUUID().toString(),
                title = title.trim().ifBlank { "${chapter.titleGujarati} કસોટી" },
                topic = ParentQuizTopic.PREPARED_BOOK,
                createdAt = now(),
                questions = questions,
                bookId = book.id,
                bookTitle = book.title,
                chapterId = chapter.id,
                chapterTitleGujarati = chapter.titleGujarati,
            )
        )
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
        val expanded = buildList {
            while (size < requested) base.forEach { if (size < requested) add(it) }
        }
        val questions = expanded.take(requested).mapIndexed { index, question ->
            question.toParentQuizQuestion(index, "કૌશલ્ય: ${concept.titleGujarati}")
        }
        return savePlan(
            ParentQuizPlan(
                id = UUID.randomUUID().toString(),
                title = title.trim().ifBlank { "${concept.titleGujarati} કસોટી" },
                topic = ParentQuizTopic.SKILL,
                createdAt = now(),
                questions = questions,
                skillConceptId = concept.id,
                skillTitleGujarati = concept.titleGujarati,
            )
        )
    }

    private suspend fun savePlan(plan: ParentQuizPlan): ParentQuizPlan {
        repository.save(plan)
        return plan
    }

    private fun com.mitra.learning.study.practice.MitraVoiceChallenge.toParentQuestion(index: Int) = ParentQuizQuestion(
        id = "$id-$index",
        promptGujarati = promptGujarati,
        spokenPrompt = spokenPrompt,
        recognitionLanguageTag = recognitionLanguageTag,
        kind = kind,
        evaluationMode = evaluationMode,
        expectedNumber = expectedNumber,
        expectedText = expectedText,
        acceptedAnswers = acceptedAnswers,
        hintGujarati = hintGujarati,
        correctionGujarati = correctionGujarati,
        sourceLabels = sourceLabels,
    )

    private fun LearningQuestion.toParentQuizQuestion(index: Int, sourceLabel: String): ParentQuizQuestion {
        val mode = when (evaluationMode) {
            EvaluationMode.NUMERIC -> MitraChallengeEvaluationMode.NUMERIC
            EvaluationMode.KEYWORD -> MitraChallengeEvaluationMode.KEYWORD
            EvaluationMode.MULTIPLE_CHOICE, EvaluationMode.SHORT_TEXT, EvaluationMode.PARTICIPATION ->
                MitraChallengeEvaluationMode.EXACT_TEXT
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
            id = "focused-${id}-$index",
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
            sourceLabels = listOfNotNull(sourceLabel, sourcePage?.let { "PDF p.$it" }),
        )
    }

    private fun isAssessable(question: LearningQuestion): Boolean = when (question.evaluationMode) {
        EvaluationMode.NUMERIC -> question.expectedAnswer != null
        EvaluationMode.MULTIPLE_CHOICE, EvaluationMode.SHORT_TEXT, EvaluationMode.KEYWORD ->
            !question.expectedText.isNullOrBlank() || question.acceptedAnswers.isNotEmpty()
        EvaluationMode.PARTICIPATION -> false
    }
}
