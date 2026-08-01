package com.mitra.learning.learning.engine

import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.learning.curriculum.BuiltInCurriculum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConceptSelectorTest {
    @Test
    fun prerequisiteBlocksAdvancedConcept() {
        val selected = ConceptSelector.select(
            concepts = BuiltInCurriculum.concepts.filter {
                it.id == BuiltInCurriculum.COUNT_1_20 || it.id == BuiltInCurriculum.COUNT_21_50
            },
            mastery = emptyList(),
            prerequisites = BuiltInCurriculum.prerequisites,
        )

        assertEquals(BuiltInCurriculum.COUNT_1_20, selected?.id)
    }

    @Test
    fun advancedConceptBecomesEligibleAfterPrerequisiteThreshold() {
        val selected = ConceptSelector.select(
            concepts = BuiltInCurriculum.concepts.filter {
                it.id == BuiltInCurriculum.COUNT_1_20 || it.id == BuiltInCurriculum.COUNT_21_50
            },
            mastery = listOf(mastery(BuiltInCurriculum.COUNT_1_20, 0.75f)),
            prerequisites = BuiltInCurriculum.prerequisites,
        )

        assertEquals(BuiltInCurriculum.COUNT_21_50, selected?.id)
    }

    @Test
    fun weakerEligibleConceptIsPreferred() {
        val selected = ConceptSelector.select(
            concepts = BuiltInCurriculum.concepts.filter {
                it.id == BuiltInCurriculum.ADD_UNDER_10 || it.id == BuiltInCurriculum.SUBTRACT_UNDER_10
            },
            mastery = listOf(
                mastery(BuiltInCurriculum.ADD_UNDER_10, 0.50f),
                mastery(BuiltInCurriculum.SUBTRACT_UNDER_10, 0.10f),
            ),
            prerequisites = emptyList(),
        )

        assertNotEquals(BuiltInCurriculum.ADD_UNDER_10, selected?.id)
        assertEquals(BuiltInCurriculum.SUBTRACT_UNDER_10, selected?.id)
    }



    @Test
    fun similarlyWeakConceptsRotateToLeastRecentlyPractised() {
        val add = BuiltInCurriculum.concepts.first { it.id == BuiltInCurriculum.ADD_UNDER_10 }
        val sub = BuiltInCurriculum.concepts.first { it.id == BuiltInCurriculum.SUBTRACT_UNDER_10 }
        val selected = ConceptSelector.select(
            concepts = listOf(add, sub),
            mastery = listOf(
                mastery(add.id, 0.20f).copy(lastPracticedAt = 10_000L),
                mastery(sub.id, 0.25f).copy(lastPracticedAt = 1_000L),
            ),
            prerequisites = emptyList(),
        )
        assertEquals(sub.id, selected?.id)
    }

    @Test
    fun bookConceptNotReadyForPracticeIsIgnored() {
        val hidden = BuiltInCurriculum.concepts.first().copy(
            id = "book-hidden",
            sortOrder = -100,
            practiceReady = false,
        )
        val ready = BuiltInCurriculum.concepts.first().copy(
            id = "ready",
            sortOrder = 1,
            practiceReady = true,
        )

        val selected = ConceptSelector.select(
            concepts = listOf(hidden, ready),
            mastery = emptyList(),
            prerequisites = emptyList(),
        )

        assertEquals("ready", selected?.id)
    }

    @Test
    fun returnsNullWhenNoConceptIsPracticeReady() {
        val hidden = BuiltInCurriculum.concepts.first().copy(
            id = "book-hidden-only",
            practiceReady = false,
        )
        val selected = ConceptSelector.select(
            concepts = listOf(hidden),
            mastery = emptyList(),
            prerequisites = emptyList(),
        )
        assertNull(selected)
    }

    @Test
    fun preparedBookConceptIsPreferredOverBuiltInFallback() {
        val builtIn = BuiltInCurriculum.concepts.first().copy(
            id = "built-in-fallback",
            builtIn = true,
            practiceReady = true,
            sortOrder = 1,
        )
        val book = BuiltInCurriculum.concepts.first().copy(
            id = "book-ready",
            builtIn = false,
            bookId = "book-1",
            chapterId = "chapter-1",
            practiceReady = true,
            sortOrder = 10000,
        )
        val selected = ConceptSelector.select(
            concepts = listOf(builtIn, book),
            mastery = emptyList(),
            prerequisites = emptyList(),
        )
        assertEquals("book-ready", selected?.id)
    }


    @Test
    fun dueSpacedReviewIsSelectedBeforeNewWeakWork() {
        val due = BuiltInCurriculum.concepts.first { it.id == BuiltInCurriculum.ADD_UNDER_10 }
        val newSkill = BuiltInCurriculum.concepts.first { it.id == BuiltInCurriculum.SUBTRACT_UNDER_10 }
        val selected = ConceptSelector.select(
            concepts = listOf(due, newSkill),
            mastery = listOf(
                mastery(due.id, 0.90f).copy(nextReviewAt = 900L, totalAttempts = 5),
            ),
            prerequisites = emptyList(),
            nowMillis = 1_000L,
        )
        assertEquals(due.id, selected?.id)
    }

    private fun mastery(id: String, value: Float) = MasteryEntity(
        conceptId = id,
        mastery = value,
        totalAttempts = 1,
        correctAttempts = 1,
        hintCount = 0,
        lastPracticedAt = 1L,
        lastSuccessAt = 1L,
    )
}
