package com.mitra.learning.learning.progress

import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.data.db.entity.SessionEntity
import com.mitra.learning.data.db.entity.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ProgressAnalyzerTest {
    private val now = 1_750_000_000_000L

    @Test
    fun `participation does not count as assessed accuracy`() {
        val concept = concept("math", "ગણિત", builtIn = false)
        val session = session("s1", concept.id, 600, now - 1_000)
        val result = ProgressAnalyzer.analyze(
            concepts = listOf(concept),
            mastery = listOf(mastery(concept.id, 0.5f, 2, 1)),
            sessions = listOf(session),
            attempts = listOf(
                attempt("a1", "s1", concept.id, AttemptResult.CORRECT),
                attempt("a2", "s1", concept.id, AttemptResult.INCORRECT),
                attempt("a3", "s1", concept.id, AttemptResult.UNKNOWN),
            ),
            nowMillis = now,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(2, result.assessedAttempts)
        assertEquals(1, result.correctAttempts)
        assertEquals(1, result.recentSessions.single().participation)
    }

    @Test
    fun `weak practiced concept appears in needs practice`() {
        val weak = concept("weak", "બાદબાકી")
        val strong = concept("strong", "ગણતરી")
        val result = ProgressAnalyzer.analyze(
            concepts = listOf(weak, strong),
            mastery = listOf(
                mastery(weak.id, 0.35f, 4, 1),
                mastery(strong.id, 0.92f, 5, 5),
            ),
            sessions = emptyList(),
            attempts = emptyList(),
            nowMillis = now,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals("weak", result.needsPractice.single().conceptId)
        assertEquals("strong", result.strongConcepts.single().conceptId)
    }

    @Test
    fun `recommendation prefers real prepared book concept`() {
        val builtIn = concept("built", "ગણતરી", builtIn = true)
        val book = concept("book", "વાંચન", builtIn = false)
        val result = ProgressAnalyzer.analyze(
            concepts = listOf(builtIn, book),
            mastery = listOf(
                mastery(builtIn.id, 0.10f, 3, 1),
                mastery(book.id, 0.70f, 2, 1),
            ),
            sessions = emptyList(),
            attempts = emptyList(),
            nowMillis = now,
            zoneId = ZoneId.of("UTC"),
        )

        assertNotNull(result.recommendation)
        assertEquals("book", result.recommendation?.conceptId)
    }

    @Test
    fun `completed session time is rounded to minutes`() {
        val concept = concept("math", "ગણિત")
        val result = ProgressAnalyzer.analyze(
            concepts = listOf(concept),
            mastery = emptyList(),
            sessions = listOf(session("s1", concept.id, 61, now - 1_000)),
            attempts = emptyList(),
            nowMillis = now,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(2, result.todayMinutes)
        assertEquals(2, result.last7DaysMinutes)
        assertTrue(result.completedSessions == 1)
    }

    @Test
    fun `recommendation respects prerequisites`() {
        val first = concept("first", "પહેલું", builtIn = false)
        val locked = concept("locked", "આગળનું", builtIn = false)
        val result = ProgressAnalyzer.analyze(
            concepts = listOf(first, locked),
            mastery = listOf(mastery(first.id, 0.40f, 3, 1)),
            prerequisites = listOf(ConceptPrerequisiteEntity(locked.id, first.id)),
            sessions = emptyList(),
            attempts = emptyList(),
            nowMillis = now,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals("first", result.recommendation?.conceptId)
    }

    private fun concept(id: String, title: String, builtIn: Boolean = false) = ConceptEntity(
        id = id,
        subject = "Mathematics",
        standard = 2,
        language = "gu-IN",
        titleGujarati = title,
        titleEnglish = null,
        descriptionGujarati = title,
        difficulty = 1,
        expectedLearningOutcome = title,
        sortOrder = 1,
        builtIn = builtIn,
        bookId = if (builtIn) null else "book-1",
        chapterId = if (builtIn) null else "chapter-1",
        sourcePageStart = 1,
        sourcePageEnd = 2,
        practiceReady = true,
    )

    private fun mastery(id: String, value: Float, attempts: Int, correct: Int) = MasteryEntity(
        conceptId = id,
        mastery = value,
        totalAttempts = attempts,
        correctAttempts = correct,
        hintCount = 0,
        lastPracticedAt = now - 2_000,
        lastSuccessAt = now - 2_000,
    )

    private fun session(id: String, conceptId: String, seconds: Int, startedAt: Long) = SessionEntity(
        id = id,
        startedAt = startedAt,
        endedAt = startedAt + seconds * 1000L,
        primaryConceptId = conceptId,
        activityCount = 3,
        durationSeconds = seconds,
        status = SessionStatus.COMPLETED,
    )

    private fun attempt(id: String, sessionId: String, conceptId: String, result: AttemptResult) = AttemptEntity(
        id = id,
        sessionId = sessionId,
        conceptId = conceptId,
        activityType = "QUESTION",
        result = result,
        hintCount = 0,
        createdAt = now - 1_000,
    )
}
