package com.mitra.learning.learning.engine

import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.PracticeContext
import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.data.db.entity.SessionEntity
import com.mitra.learning.data.db.entity.SessionStatus
import com.mitra.learning.data.repository.BookKnowledgeRepository
import com.mitra.learning.data.repository.BookRepository
import com.mitra.learning.data.repository.LearningRepository
import com.mitra.learning.learning.activity.ActivityPlanPolicy
import com.mitra.learning.learning.activity.QuestionVarietyPolicy
import com.mitra.learning.learning.curriculum.BuiltInCurriculum
import com.mitra.learning.learning.curriculum.Standard2SkillActivityFactory
import com.mitra.learning.learning.evaluation.ActivityEvaluator
import com.mitra.learning.learning.evaluation.MasteryPolicy
import com.mitra.learning.learning.evaluation.MathMistakeClassifier
import com.mitra.learning.learning.evaluation.SpacedReviewPolicy
import com.mitra.learning.learning.evaluation.GujaratiNumberNormalizer
import com.mitra.learning.learning.model.AnswerFeedback
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.learning.model.SessionPlan
import com.mitra.learning.learning.model.SessionSummary
import com.mitra.learning.learning.offline.OfflineQuestionBank
import java.util.UUID

class DefaultLearningEngine(
    private val repository: LearningRepository,
    private val aiGateway: AiGateway,
    private val bookKnowledgeRepository: BookKnowledgeRepository? = null,
    private val bookRepository: BookRepository? = null,
    private val questionBank: OfflineQuestionBank? = null,
    private val now: () -> Long = { System.currentTimeMillis() },
) : LearningEngine {

    override suspend fun startSession(questionCount: Int): SessionPlan? =
        startSingleConceptSession(questionCount.coerceIn(1, 8))

    override suspend fun startConceptSession(conceptId: String, questionCount: Int): SessionPlan? {
        repository.seedBuiltInCurriculumIfNeeded()
        val concept = repository.getConcepts().firstOrNull { it.id == conceptId && it.practiceReady } ?: return null
        val recent = repository.recentQuestionFingerprints(60).toSet()
        val requested = questionCount.coerceIn(1, 8)
        val bank = if (!concept.builtIn) questionBank?.load(concept.id, requested, recent).orEmpty() else emptyList()
        val generated = if (bank.size >= requested) emptyList() else aiGateway.createPracticeQuestions(
            concept = concept,
            count = (requested * 2).coerceIn(requested, 8),
            context = buildPracticeContext(concept, recent),
        ).map { it.copy(conceptId = it.conceptId ?: concept.id) }
        if (!concept.builtIn && generated.isNotEmpty()) questionBank?.save(concept.id, generated)
        val questions = ActivityPlanPolicy.apply(
            QuestionVarietyPolicy.select((bank + generated).distinctBy { it.fingerprint }, requested, recent)
        ).take(requested)
        if (questions.isEmpty()) return null
        val session = createSession(concept.id)
        return SessionPlan(session.id, concept, questions)
    }

    /**
     * Skill mode is deliberately mixed. It always includes two-digit work and a carry challenge,
     * rather than asking six copies of the same skill every time.
     */
    override suspend fun startSkillSession(questionCount: Int): SessionPlan? {
        repository.seedBuiltInCurriculumIfNeeded()
        val conceptsById = repository.getConcepts().filter { it.builtIn }.associateBy { it.id }
        val recent = repository.recentQuestionFingerprints(60).toMutableSet()
        val tableId = listOf(
            BuiltInCurriculum.TABLE_2, BuiltInCurriculum.TABLE_3, BuiltInCurriculum.TABLE_4,
            BuiltInCurriculum.TABLE_5, BuiltInCurriculum.TABLE_6, BuiltInCurriculum.TABLE_7,
            BuiltInCurriculum.TABLE_8, BuiltInCurriculum.TABLE_9, BuiltInCurriculum.TABLE_10,
        )[((now() / 86_400_000L) % 9L).toInt()]
        val requestedIds = listOf(
            BuiltInCurriculum.ADD_2D_2D_NO_CARRY,
            BuiltInCurriculum.ADD_WITH_CARRY,
            BuiltInCurriculum.ADD_WITH_CARRY,
            BuiltInCurriculum.SUB_WITH_BORROW,
            BuiltInCurriculum.WORD_PROBLEMS,
            tableId,
            BuiltInCurriculum.GUJ_SPELLING,
            BuiltInCurriculum.ENG_SENTENCE_COMPLETION,
        )
        val target = questionCount.coerceIn(1, 8)
        val questions = mutableListOf<LearningQuestion>()
        requestedIds.forEachIndexed { index, id ->
            if (questions.size >= target) return@forEachIndexed
            val concept = conceptsById[id] ?: return@forEachIndexed
            val generated = Standard2SkillActivityFactory.create(
                concept = concept,
                count = 1,
                seed = now() + index * 9_973L,
                excludedFingerprints = recent,
            ).ifEmpty {
                Standard2SkillActivityFactory.create(concept, 1, now() + index * 19_997L)
            }
            generated.firstOrNull()?.let { question ->
                val tagged = question.copy(conceptId = concept.id)
                if (tagged.fingerprint !in questions.map { it.fingerprint }.toSet()) {
                    questions += tagged
                    recent += tagged.fingerprint
                }
            }
        }
        if (questions.isEmpty()) return null

        val primary = conceptsById[BuiltInCurriculum.ADD_2D_2D_NO_CARRY] ?: conceptsById.values.first()
        val displayConcept = primary.copy(titleGujarati = "મિશ્ર ગણિત ચેલેન્જ")
        val safePlan = ActivityPlanPolicy.apply(questions).take(target)
        val session = createSession(primary.id)
        return SessionPlan(session.id, displayConcept, safePlan)
    }

    private suspend fun startSingleConceptSession(questionCount: Int): SessionPlan? {
        repository.seedBuiltInCurriculumIfNeeded()
        val allConcepts = repository.getConcepts()
        val mastery = repository.getMastery()
        val prerequisites = repository.getPrerequisites()
        var concept = ConceptSelector.select(allConcepts, mastery, prerequisites) ?: return null
        val recent = repository.recentQuestionFingerprints(60).toSet()

        suspend fun activitiesFor(selected: ConceptEntity): List<LearningQuestion> {
            val bankQuestions = if (!selected.builtIn) {
                questionBank?.load(selected.id, questionCount, recent).orEmpty()
            } else emptyList()
            if (bankQuestions.size >= questionCount) {
                return ActivityPlanPolicy.apply(bankQuestions).take(questionCount)
            }

            val generationCount = (questionCount * 2).coerceIn(questionCount, 8)
            val generated = aiGateway.createPracticeQuestions(
                concept = selected,
                count = generationCount,
                context = buildPracticeContext(selected, recent),
            ).map { it.copy(conceptId = it.conceptId ?: selected.id) }
            if (!selected.builtIn && generated.isNotEmpty()) questionBank?.save(selected.id, generated)
            val combined = (bankQuestions + generated).distinctBy { it.fingerprint }
            val varied = QuestionVarietyPolicy.select(combined, questionCount, recent)
            return ActivityPlanPolicy.apply(varied).take(questionCount)
        }

        var activities = runCatching { activitiesFor(concept) }.getOrElse { failure ->
            if (concept.builtIn) throw failure
            val fallback = ConceptSelector.select(
                concepts = allConcepts.filter { it.builtIn },
                mastery = mastery,
                prerequisites = prerequisites,
            ) ?: throw failure
            concept = fallback
            activitiesFor(fallback)
        }

        if (activities.isEmpty() && !concept.builtIn) {
            ConceptSelector.select(allConcepts.filter { it.builtIn }, mastery, prerequisites)?.let { fallback ->
                concept = fallback
                activities = activitiesFor(fallback)
            }
        }
        if (activities.isEmpty()) return null

        val session = createSession(concept.id)
        return SessionPlan(session.id, concept, activities)
    }

    private suspend fun createSession(primaryConceptId: String): SessionEntity {
        val session = SessionEntity(
            id = UUID.randomUUID().toString(),
            startedAt = now(),
            endedAt = null,
            primaryConceptId = primaryConceptId,
            activityCount = 0,
            durationSeconds = 0,
            status = SessionStatus.ACTIVE,
        )
        repository.insertSession(session)
        return session
    }

    override suspend fun submitAnswer(
        sessionId: String,
        conceptId: String,
        question: LearningQuestion,
        answerText: String,
        hintsUsed: Int,
    ): AnswerFeedback {
        require(question.evaluationMode != EvaluationMode.PARTICIPATION) {
            "Participation activities must be completed with completeParticipation()."
        }
        return recordResult(
            sessionId = sessionId,
            conceptId = question.conceptId ?: conceptId,
            question = question,
            result = ActivityEvaluator.evaluate(question, answerText),
            hintsUsed = hintsUsed,
            answerText = answerText,
        )
    }

    override suspend fun completeParticipation(
        sessionId: String,
        conceptId: String,
        question: LearningQuestion,
    ): AnswerFeedback = recordResult(
        sessionId = sessionId,
        conceptId = question.conceptId ?: conceptId,
        question = question,
        result = AttemptResult.UNKNOWN,
        hintsUsed = 0,
    )

    override suspend fun skipQuestion(
        sessionId: String,
        conceptId: String,
        question: LearningQuestion,
    ): AnswerFeedback = recordResult(
        sessionId = sessionId,
        conceptId = question.conceptId ?: conceptId,
        question = question,
        result = AttemptResult.SKIPPED,
        hintsUsed = 0,
    )

    override suspend fun completeSession(sessionId: String, conceptTitleGujarati: String): SessionSummary {
        val session = requireNotNull(repository.findSession(sessionId)) { "Session not found" }
        val end = now()
        val attempts = repository.attemptsForSession(sessionId)
        val mastery = session.primaryConceptId?.let { repository.getMastery(it)?.mastery } ?: 0f
        val assessed = attempts.filter { it.result != AttemptResult.UNKNOWN }
        repository.updateSession(
            session.copy(
                endedAt = end,
                durationSeconds = ((end - session.startedAt) / 1000L).coerceAtLeast(0L).toInt(),
                activityCount = attempts.size,
                status = SessionStatus.COMPLETED,
            )
        )
        return SessionSummary(
            conceptTitleGujarati = conceptTitleGujarati,
            attempts = attempts.size,
            correct = assessed.count { it.result == AttemptResult.CORRECT },
            mastery = mastery,
            assessed = assessed.size,
            participationActivities = attempts.count { it.result == AttemptResult.UNKNOWN },
        )
    }

    override suspend fun stopSession(sessionId: String) {
        val session = repository.findSession(sessionId) ?: return
        val end = now()
        val attempts = repository.attemptsForSession(sessionId)
        repository.updateSession(
            session.copy(
                endedAt = end,
                durationSeconds = ((end - session.startedAt) / 1000L).coerceAtLeast(0L).toInt(),
                activityCount = attempts.size,
                status = SessionStatus.STOPPED,
            )
        )
    }

    private suspend fun recordResult(
        sessionId: String,
        conceptId: String,
        question: LearningQuestion,
        result: AttemptResult,
        hintsUsed: Int,
        answerText: String? = null,
    ): AnswerFeedback {
        val timestamp = now()
        val current = repository.getMastery(conceptId) ?: MasteryEntity(
            conceptId = conceptId,
            mastery = 0f,
            totalAttempts = 0,
            correctAttempts = 0,
            hintCount = 0,
            lastPracticedAt = null,
            lastSuccessAt = null,
        )

        repository.insertAttempt(
            AttemptEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                conceptId = conceptId,
                activityType = question.activityType,
                result = result,
                hintCount = hintsUsed,
                createdAt = timestamp,
                questionFingerprint = question.fingerprint,
            )
        )

        val assessed = result != AttemptResult.UNKNOWN
        val nextMastery = if (assessed) MasteryPolicy.update(current.mastery, result, hintsUsed) else current.mastery
        val review = SpacedReviewPolicy.update(
            result = result,
            currentIntervalDays = current.reviewIntervalDays,
            currentSuccesses = current.consecutiveSuccesses,
            nowMillis = timestamp,
        )
        repository.upsertMastery(
            current.copy(
                mastery = nextMastery,
                totalAttempts = current.totalAttempts + if (assessed) 1 else 0,
                correctAttempts = current.correctAttempts + if (result == AttemptResult.CORRECT) 1 else 0,
                hintCount = current.hintCount + if (assessed) hintsUsed else 0,
                lastPracticedAt = timestamp,
                lastSuccessAt = if (result == AttemptResult.CORRECT) timestamp else current.lastSuccessAt,
                nextReviewAt = if (assessed) review.nextReviewAt else current.nextReviewAt,
                reviewIntervalDays = if (assessed) review.intervalDays else current.reviewIntervalDays,
                consecutiveSuccesses = if (assessed) review.consecutiveSuccesses else current.consecutiveSuccesses,
            )
        )

        return buildFeedback(result, question, answerText, nextMastery)
    }

    private fun buildFeedback(
        result: AttemptResult,
        question: LearningQuestion,
        answerText: String?,
        mastery: Float,
    ): AnswerFeedback {
        if (result == AttemptResult.INCORRECT && question.evaluationMode == EvaluationMode.NUMERIC) {
            val expected = question.expectedAnswer
            val child = answerText?.let(GujaratiNumberNormalizer::parseInt)
            val work = question.arithmeticWork
            if (expected != null && child != null && work != null) {
                val mistake = MathMistakeClassifier.classify(work, child, expected)
                return AnswerFeedback(
                    result = result,
                    messageGujarati = "ફરી પ્રયત્ન કરીએ. ${mistake.hintGujarati}",
                    expectedAnswer = expected,
                    mastery = mastery,
                    retrySuggested = true,
                    mistakeCode = mistake.code.name,
                )
            }
        }

        val message = when (result) {
            AttemptResult.UNKNOWN -> "સરસ. હવે આગળ શું શોધીએ તે જોઈએ."
            AttemptResult.CORRECT -> "હા! સાચું. રફ કામમાં લીધેલા પગલાં યાદ રાખજો."
            AttemptResult.PARTIAL -> "લગભગ સાચું. એક નાની મદદ લઈને ફરી અજમાવીએ."
            AttemptResult.SKIPPED -> "ઠીક છે. આ પ્રવૃત્તિ પછી ફરી અજમાવીશું."
            AttemptResult.INCORRECT -> question.hintGujarati
                ?.takeIf { it.isNotBlank() }
                ?.let { "ફરી વિચારીએ. સંકેત: $it" }
                ?: "ફરી વિચારીએ. એક વાર ધીમે વાંચીને ફરી અજમાવો."
        }
        return AnswerFeedback(result, message, question.expectedAnswer, mastery)
    }

    private suspend fun buildPracticeContext(concept: ConceptEntity, recent: Set<String>): PracticeContext {
        val chapterId = concept.chapterId
        if (chapterId == null || bookKnowledgeRepository == null) {
            return PracticeContext(recentQuestionFingerprints = recent)
        }
        val chapter = bookKnowledgeRepository.getChapter(chapterId)
            ?: return PracticeContext(recentQuestionFingerprints = recent)
        val pageStart = concept.sourcePageStart ?: chapter.startPage
        val pageEnd = concept.sourcePageEnd ?: chapter.endPage
        val grounded = bookKnowledgeRepository.pageKnowledge(chapterId)
            .filter { it.pageNumber in pageStart..pageEnd }
            .joinToString("\n\n") { page ->
                buildString {
                    append("Page ${page.pageNumber}: ${page.summaryGujarati}")
                    page.visibleTextGujarati?.takeIf { it.isNotBlank() }?.let { append("\nVisible text: $it") }
                    page.exercisesJson?.takeIf { it.isNotBlank() }?.let { append("\nExercises: $it") }
                }
            }.take(12_000)
        return PracticeContext(
            bookTitle = concept.bookId?.let { bookRepository?.getBook(it)?.title },
            chapterTitleGujarati = chapter.titleGujarati,
            groundedBookText = grounded,
            recentQuestionFingerprints = recent,
        )
    }
}
