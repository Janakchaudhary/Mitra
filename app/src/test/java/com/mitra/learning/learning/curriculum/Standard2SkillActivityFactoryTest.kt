package com.mitra.learning.learning.curriculum

import com.mitra.learning.learning.evaluation.ActivityEvaluator
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Standard2SkillActivityFactoryTest {
    @Test
    fun curriculumContainsSeparateStandard2Skills() {
        val ids = BuiltInCurriculum.concepts.map { it.id }.toSet()
        listOf(
            BuiltInCurriculum.ADD_2D_2D_NO_CARRY,
            BuiltInCurriculum.ADD_WITH_CARRY,
            BuiltInCurriculum.SUB_WITH_BORROW,
            BuiltInCurriculum.MULTIPLICATION_MEANING,
            BuiltInCurriculum.TABLE_2,
            BuiltInCurriculum.TABLE_10,
            BuiltInCurriculum.GUJ_SPELLING,
            BuiltInCurriculum.GUJ_SINGULAR_PLURAL,
            BuiltInCurriculum.ENG_SPELLING,
            BuiltInCurriculum.ENG_READ_ALOUD,
        ).forEach { assertTrue("Missing $it", it in ids) }
    }

    @Test
    fun everyMultiplicationTableHasItsOwnConceptAndQuestions() {
        val tableIds = listOf(
            BuiltInCurriculum.TABLE_2, BuiltInCurriculum.TABLE_3, BuiltInCurriculum.TABLE_4,
            BuiltInCurriculum.TABLE_5, BuiltInCurriculum.TABLE_6, BuiltInCurriculum.TABLE_7,
            BuiltInCurriculum.TABLE_8, BuiltInCurriculum.TABLE_9, BuiltInCurriculum.TABLE_10,
        )
        assertEquals(9, tableIds.distinct().size)
        tableIds.forEach { id ->
            val concept = requireNotNull(BuiltInCurriculum.concepts.find { it.id == id })
            val activities = Standard2SkillActivityFactory.create(concept, 5)
            assertEquals(5, activities.size)
            assertTrue(activities.all { it.type in setOf(ActivityType.TABLES, ActivityType.WORD_PROBLEM) })
            assertTrue(activities.any { it.type == ActivityType.TABLES })
            assertTrue(activities.any { it.type == ActivityType.WORD_PROBLEM })
            assertTrue(activities.all { it.evaluationMode == EvaluationMode.NUMERIC })
            assertTrue(activities.all { it.expectedAnswer != null })
            assertTrue(activities.all { it.conceptId == id })
        }
    }

    @Test
    fun carryingAndBorrowingAreSeparateMasteryConcepts() {
        assertFalse(BuiltInCurriculum.ADD_WITH_CARRY == BuiltInCurriculum.ADD_2D_2D_NO_CARRY)
        assertFalse(BuiltInCurriculum.SUB_WITH_BORROW == BuiltInCurriculum.SUB_2D_2D_NO_BORROW)
        assertNotNull(BuiltInCurriculum.concepts.find { it.id == BuiltInCurriculum.ADD_WITH_CARRY })
        assertNotNull(BuiltInCurriculum.concepts.find { it.id == BuiltInCurriculum.SUB_WITH_BORROW })
    }

    @Test
    fun spellingHidesAnswerOnScreenButHasSpokenDictation() {
        val concept = requireNotNull(BuiltInCurriculum.concepts.find { it.id == BuiltInCurriculum.GUJ_SPELLING })
        val activity = Standard2SkillActivityFactory.create(concept, 1).single()
        assertEquals(ActivityType.SPELLING, activity.type)
        assertEquals(EvaluationMode.SHORT_TEXT, activity.evaluationMode)
        val answer = requireNotNull(activity.expectedText)
        assertFalse(activity.promptGujarati.contains(answer))
        assertTrue(activity.speechTextGujarati.contains(answer))
        assertEquals(com.mitra.learning.data.db.entity.AttemptResult.CORRECT, ActivityEvaluator.evaluate(activity, answer))
    }


    @Test
    fun carryQuestionsAreVariedAndExposeRoughWorkMetadata() {
        val concept = requireNotNull(BuiltInCurriculum.concepts.find { it.id == BuiltInCurriculum.ADD_WITH_CARRY })
        val activities = Standard2SkillActivityFactory.create(concept, 6, seed = 1234L)
        assertEquals(6, activities.size)
        assertEquals(6, activities.map { it.fingerprint }.distinct().size)
        assertTrue(activities.all { it.arithmeticWork != null })
        assertTrue(activities.all { it.arithmeticWork?.regrouping == true })
        assertTrue(activities.all { activity ->
            val work = requireNotNull(activity.arithmeticWork)
            (work.top % 10 + work.bottom % 10) >= 10
        })
    }

    @Test
    fun twentyQuestionSkillSetIsUnique() {
        val concept = requireNotNull(BuiltInCurriculum.concepts.find { it.id == BuiltInCurriculum.ADD_WITH_CARRY })
        val activities = Standard2SkillActivityFactory.create(concept, 20, seed = 2020L)
        assertEquals(20, activities.size)
        assertEquals(20, activities.map { it.fingerprint }.distinct().size)
    }

    @Test
    fun twoDigitSkillProducesTwoDigitArithmetic() {
        val concept = requireNotNull(BuiltInCurriculum.concepts.find { it.id == BuiltInCurriculum.ADD_2D_2D_NO_CARRY })
        val activities = Standard2SkillActivityFactory.create(concept, 5)
        assertTrue(activities.all { it.expectedAnswer != null && it.expectedAnswer!! >= 20 })
    }
}
