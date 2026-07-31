package com.mitra.learning.learning.engine

import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity
import com.mitra.learning.learning.curriculum.BuiltInCurriculum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
