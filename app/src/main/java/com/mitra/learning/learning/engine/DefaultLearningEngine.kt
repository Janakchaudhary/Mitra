package com.mitra.learning.learning.engine

import com.mitra.learning.ai.AiGateway
import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.data.db.entity.SessionEntity
import com.mitra.learning.data.db.entity.SessionStatus
import com.mitra.learning.data.repository.BookKnowledgeRepository
import com.mitra.learning.data.repository.BookRepository
import com.mitra.learning.data.repository.LearningRepository
import com.mitra.learning.learning.evaluation.GujaratiNumberNormalizer
import com.mitra.learning.learning.evaluation.MasteryPolicy
import com.mitra.learning.ai.PracticeContext
import com.mitra.learning.learning.model.AnswerFeedback
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.learning.model.SessionPlan
import com.mitra.learning.learning.model.SessionSummary
import java.util.UUID

class DefaultLearningEngine(
    private val repository: LearningRepository,
    private val aiGateway: AiGateway,
    private val bookKnowledgeRepository: BookKnowledgeRepository? = null,
    private val bookRepository: BookRepository? = null,
    private val now: () -> Long = { System.currentTimeMillis() },
) : LearningEngine {

    override suspend fun startSession(questionCount: Int): SessionPlan? {
        repository.seedBuiltInCurriculumIfNeeded()

        val concept = ConceptSelector.select(
            concepts = repository.getConcepts(),
            mastery = repository.getMastery(),
            prerequisites = repository.getPrerequisites(),
        ) ?: return null

        val session = SessionEntity(
            id = UUID.randomUUID().toString(),
            startedAt = now(),
            endedAt = null,
            primaryConceptId = concept.id,
            activityCount = 0,
            durationSeconds = 0,
            status = SessionStatus.ACTIVE,
        )
        repository.insertSession(session)

        val questions = aiGateway.createPracticeQuestions(
            concept = concept,
            count = questionCount.coerceAtLeast(1),
            context = buildPracticeContext(concept),
        )
        return SessionPlan(session.id, concept, questions)
    }

    override suspend fun submitAnswer(
        sessionId: String,
        conceptId: String,
        question: LearningQuestion,
        answerText: String,
        hintsUsed: Int,
    ): AnswerFeedback {
        val parsed = GujaratiNumberNormalizer.parseInt(answerText)
        val result = if (parsed != null && parsed == question.expectedAnswer) {
            AttemptResult.CORRECT
        } else {
            AttemptResult.INCORRECT
        }
        return recordResult(sessionId, conceptId, question, result, hintsUsed)
    }

    override suspend fun skipQuestion(
        sessionId: String,
        conceptId: String,
        question: LearningQuestion,
    ): AnswerFeedback = recordResult(
        sessionId = sessionId,
        conceptId = conceptId,
        question = question,
        result = AttemptResult.SKIPPED,
        hintsUsed = 0,
    )

    override suspend fun completeSession(sessionId: String, conceptTitleGujarati: String): SessionSummary {
        val session = requireNotNull(repository.findSession(sessionId)) { "Session not found" }
        val end = now()
        val attempts = repository.attemptsForSession(sessionId)
        val mastery = session.primaryConceptId?.let { repository.getMastery(it)?.mastery } ?: 0f

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
            correct = attempts.count { it.result == AttemptResult.CORRECT },
            mastery = mastery,
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
    ): AnswerFeedback {
        val timestamp = now()
        val current = repository.getMastery(conceptId)
            ?: MasteryEntity(
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
            )
        )

        val nextMastery = MasteryPolicy.update(current.mastery, result, hintsUsed)
        val updated = current.copy(
            mastery = nextMastery,
            totalAttempts = current.totalAttempts + 1,
            correctAttempts = current.correctAttempts + if (result == AttemptResult.CORRECT) 1 else 0,
            hintCount = current.hintCount + hintsUsed,
            lastPracticedAt = timestamp,
            lastSuccessAt = if (result == AttemptResult.CORRECT) timestamp else current.lastSuccessAt,
        )
        repository.upsertMastery(updated)

        return AnswerFeedback(
            result = result,
            messageGujarati = aiGateway.feedbackGujarati(result, question.expectedAnswer),
            expectedAnswer = question.expectedAnswer,
            mastery = nextMastery,
        )
    }
    private suspend fun buildPracticeContext(concept: com.mitra.learning.data.db.entity.ConceptEntity): PracticeContext? {
        val chapterId = concept.chapterId ?: return null
        val knowledgeRepository = bookKnowledgeRepository ?: return null
        val chapter = knowledgeRepository.getChapter(chapterId) ?: return null
        val pageStart = concept.sourcePageStart ?: chapter.startPage
        val pageEnd = concept.sourcePageEnd ?: chapter.endPage
        val grounded = knowledgeRepository.pageKnowledge(chapterId)
            .filter { it.pageNumber in pageStart..pageEnd }
            .joinToString("\n\n") { page ->
                buildString {
                    append("Page ${page.pageNumber}: ")
                    append(page.summaryGujarati)
                    page.visibleTextGujarati?.takeIf { it.isNotBlank() }?.let {
                        append("\nVisible text: ")
                        append(it)
                    }
                    page.exercisesJson?.takeIf { it.isNotBlank() }?.let {
                        append("\nExercises: ")
                        append(it)
                    }
                }
            }
            .take(12_000)
        val bookTitle = concept.bookId?.let { bookRepository?.getBook(it)?.title }
        return PracticeContext(
            bookTitle = bookTitle,
            chapterTitleGujarati = chapter.titleGujarati,
            groundedBookText = grounded,
        )
    }

}
