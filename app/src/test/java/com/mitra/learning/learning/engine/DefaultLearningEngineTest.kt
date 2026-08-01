package com.mitra.learning.learning.engine

import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.MockAiGateway
import com.mitra.learning.ai.PracticeContext
import com.mitra.learning.books.analysis.ChapterAnalysisRequest
import com.mitra.learning.books.analysis.ChapterAnalysisResult
import com.mitra.learning.books.analysis.TocAnalysisRequest
import com.mitra.learning.books.analysis.TocAnalysisResult
import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.data.db.entity.SessionEntity
import com.mitra.learning.data.db.entity.SessionStatus
import com.mitra.learning.data.repository.LearningRepository
import com.mitra.learning.learning.curriculum.BuiltInCurriculum
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLearningEngineTest {
    @Test
    fun correctAnswerCreatesAttemptAndUpdatesMastery() = runTest {
        val repo = FakeLearningRepository()
        val engine = DefaultLearningEngine(repo, MockAiGateway(), now = { 1_000L })
        val plan = requireNotNull(engine.startSession(questionCount = 1))
        val question = plan.questions.single()

        val feedback = engine.submitAnswer(
            sessionId = plan.sessionId,
            conceptId = plan.concept.id,
            question = question,
            answerText = question.expectedAnswer.toString(),
        )

        assertEquals(AttemptResult.CORRECT, feedback.result)
        assertEquals(0.08f, feedback.mastery, 0.0001f)
        assertEquals(1, repo.attempts.size)
        assertEquals(1, repo.masteries[plan.concept.id]?.correctAttempts)
    }

    @Test
    fun completingSessionPersistsSummaryAndStatus() = runTest {
        var clock = 1_000L
        val repo = FakeLearningRepository()
        val engine = DefaultLearningEngine(repo, MockAiGateway(), now = { clock })
        val plan = requireNotNull(engine.startSession(questionCount = 1))
        val question = plan.questions.single()
        engine.submitAnswer(plan.sessionId, plan.concept.id, question, question.expectedAnswer.toString())
        clock = 11_000L

        val summary = engine.completeSession(plan.sessionId, plan.concept.titleGujarati)

        assertEquals(1, summary.correct)
        assertEquals(1, summary.assessed)
        assertEquals(SessionStatus.COMPLETED, repo.sessions[plan.sessionId]?.status)
        assertEquals(10, repo.sessions[plan.sessionId]?.durationSeconds)
    }

    @Test
    fun participationIsRecordedWithoutIncreasingMastery() = runTest {
        val repo = FakeLearningRepository()
        val engine = DefaultLearningEngine(repo, MockAiGateway(), now = { 1_000L })
        val plan = requireNotNull(engine.startSession(questionCount = 6))
        val activity = plan.questions.first { it.evaluationMode == EvaluationMode.PARTICIPATION }

        val feedback = engine.completeParticipation(plan.sessionId, plan.concept.id, activity)

        assertEquals(AttemptResult.UNKNOWN, feedback.result)
        assertEquals(0f, feedback.mastery, 0.0001f)
        assertEquals(0, repo.masteries[plan.concept.id]?.totalAttempts)
        assertEquals(1, repo.attempts.size)
    }
    @Test
    fun skillSessionIgnoresPreparedBookConceptAndUsesOfflineCurriculum() = runTest {
        val repo = FakeLearningRepository()
        repo.concepts += BuiltInCurriculum.concepts.first().copy(
            id = "book-priority",
            titleGujarati = "પુસ્તકનો પાઠ",
            builtIn = false,
            bookId = "book-1",
            sortOrder = -100,
            practiceReady = true,
        )
        val engine = DefaultLearningEngine(repo, MockAiGateway(), now = { 1_000L })

        val plan = requireNotNull(engine.startSkillSession(questionCount = 3))

        assertTrue(plan.concept.builtIn)
        assertEquals(3, plan.questions.size)
    }


    @Test
    fun skillSessionMixesConceptsAndIncludesCarryWork() = runTest {
        val repo = FakeLearningRepository()
        val engine = DefaultLearningEngine(repo, MockAiGateway(), now = { 86_400_000L })

        val plan = requireNotNull(engine.startSkillSession(questionCount = 6))

        assertEquals(6, plan.questions.size)
        assertTrue(plan.questions.mapNotNull { it.conceptId }.distinct().size >= 4)
        assertTrue(plan.questions.any { it.arithmeticWork?.regrouping == true })
        assertEquals(plan.questions.size, plan.questions.map { it.fingerprint }.distinct().size)
    }

    @Test
    fun remoteBookFailureFallsBackToBuiltInCurriculum() = runTest {
        val repo = FakeLearningRepository()
        repo.concepts += BuiltInCurriculum.concepts.first().copy(
            id = "book-concept",
            titleGujarati = "પુસ્તક પાઠ",
            builtIn = false,
            bookId = "book-1",
            chapterId = null,
            sortOrder = -100,
            practiceReady = true,
        )
        val mock = MockAiGateway()
        val failingForBook = object : AiGateway {
            override suspend fun analyzeTableOfContents(request: TocAnalysisRequest): TocAnalysisResult =
                mock.analyzeTableOfContents(request)

            override suspend fun analyzeChapter(request: ChapterAnalysisRequest): ChapterAnalysisResult =
                mock.analyzeChapter(request)

            override suspend fun createPracticeQuestions(
                concept: ConceptEntity,
                count: Int,
                context: PracticeContext?,
            ): List<LearningQuestion> {
                if (!concept.builtIn) error("offline")
                return mock.createPracticeQuestions(concept, count, context)
            }

            override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int?): String =
                mock.feedbackGujarati(result, expectedAnswer)
        }

        val engine = DefaultLearningEngine(repo, failingForBook, now = { 1_000L })
        val plan = requireNotNull(engine.startSession(questionCount = 1))

        assertTrue(plan.concept.builtIn)
        assertEquals(1, plan.questions.size)
    }

}

private class FakeLearningRepository : LearningRepository {
    val concepts = BuiltInCurriculum.concepts.toMutableList()
    val prerequisites = BuiltInCurriculum.prerequisites.toMutableList()
    val masteries = mutableMapOf<String, MasteryEntity>()
    val sessions = mutableMapOf<String, SessionEntity>()
    val attempts = mutableListOf<AttemptEntity>()

    private val conceptFlow = MutableStateFlow(concepts.toList())
    private val masteryFlow = MutableStateFlow(emptyList<MasteryEntity>())

    override fun observeConcepts(): Flow<List<ConceptEntity>> = conceptFlow
    override fun observeMastery(): Flow<List<MasteryEntity>> = masteryFlow
    override suspend fun getConcepts(): List<ConceptEntity> = concepts
    override suspend fun getPrerequisites(): List<ConceptPrerequisiteEntity> = prerequisites
    override suspend fun getMastery(): List<MasteryEntity> = masteries.values.toList()
    override suspend fun getMastery(conceptId: String): MasteryEntity? = masteries[conceptId]
    override suspend fun seedBuiltInCurriculumIfNeeded() = Unit

    override suspend fun upsertMastery(mastery: MasteryEntity) {
        masteries[mastery.conceptId] = mastery
        masteryFlow.value = masteries.values.toList()
    }

    override suspend fun insertSession(session: SessionEntity) {
        sessions[session.id] = session
    }

    override suspend fun updateSession(session: SessionEntity) {
        sessions[session.id] = session
    }

    override suspend fun findSession(id: String): SessionEntity? = sessions[id]

    override suspend fun insertAttempt(attempt: AttemptEntity) {
        attempts += attempt
    }

    override suspend fun attemptsForSession(sessionId: String): List<AttemptEntity> =
        attempts.filter { it.sessionId == sessionId }

    override suspend fun recentQuestionFingerprints(limit: Int): List<String> =
        attempts.asReversed().map { it.questionFingerprint }.filter { it.isNotBlank() }.take(limit)
}
