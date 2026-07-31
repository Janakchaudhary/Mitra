package com.mitra.learning.learning.curriculum

import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity

object BuiltInCurriculum {
    const val COUNT_1_20 = "builtin-math-count-1-20"
    const val COUNT_21_50 = "builtin-math-count-21-50"
    const val ADD_UNDER_10 = "builtin-math-add-under-10"
    const val ADD_UNDER_20 = "builtin-math-add-under-20"
    const val SUBTRACT_UNDER_10 = "builtin-math-subtract-under-10"

    val concepts: List<ConceptEntity> = listOf(
        concept(
            id = COUNT_1_20,
            order = 10,
            gujarati = "૧ થી ૨૦ સુધી ગણતરી",
            english = "Counting 1 to 20",
            description = "૧ થી ૨૦ સુધીના અંકો ઓળખવા અને ક્રમમાં કહેવા.",
            outcome = "બાળક ૧ થી ૨૦ સુધીના અંકો ઓળખી અને ક્રમમાં કહી શકે.",
            difficulty = 1,
        ),
        concept(
            id = ADD_UNDER_10,
            order = 20,
            gujarati = "૧૦ સુધી સરવાળો",
            english = "Addition under 10",
            description = "બે નાના અંકોનો સરવાળો કરીને ૧૦ સુધીનો જવાબ શોધવો.",
            outcome = "બાળક ૧૦ સુધીના સરળ સરવાળા ઉકેલી શકે.",
            difficulty = 1,
        ),
        concept(
            id = SUBTRACT_UNDER_10,
            order = 30,
            gujarati = "૧૦ સુધી બાદબાકી",
            english = "Subtraction under 10",
            description = "૧૦ સુધીના અંકોમાં સરળ બાદબાકી સમજવી.",
            outcome = "બાળક ૧૦ સુધીની સરળ બાદબાકી ઉકેલી શકે.",
            difficulty = 1,
        ),
        concept(
            id = COUNT_21_50,
            order = 40,
            gujarati = "૨૧ થી ૫૦ સુધી ગણતરી",
            english = "Counting 21 to 50",
            description = "૨૧ થી ૫૦ સુધીના અંકો ઓળખવા અને પહેલાં/પછીનો અંક શોધવો.",
            outcome = "બાળક ૨૧ થી ૫૦ સુધીના અંકોનો ક્રમ સમજી શકે.",
            difficulty = 2,
        ),
        concept(
            id = ADD_UNDER_20,
            order = 50,
            gujarati = "૨૦ સુધી સરવાળો",
            english = "Addition under 20",
            description = "બે અંકોનો સરવાળો કરીને ૨૦ સુધીનો જવાબ શોધવો.",
            outcome = "બાળક ૨૦ સુધીના સરળ સરવાળા ઉકેલી શકે.",
            difficulty = 2,
        ),
    )

    val prerequisites: List<ConceptPrerequisiteEntity> = listOf(
        ConceptPrerequisiteEntity(COUNT_21_50, COUNT_1_20),
        ConceptPrerequisiteEntity(ADD_UNDER_20, ADD_UNDER_10),
    )

    private fun concept(
        id: String,
        order: Int,
        gujarati: String,
        english: String,
        description: String,
        outcome: String,
        difficulty: Int,
    ) = ConceptEntity(
        id = id,
        subject = "Mathematics",
        standard = 2,
        language = "gu-IN",
        titleGujarati = gujarati,
        titleEnglish = english,
        descriptionGujarati = description,
        difficulty = difficulty,
        expectedLearningOutcome = outcome,
        sortOrder = order,
        builtIn = true,
        bookId = null,
        chapterId = null,
        sourcePageStart = null,
        sourcePageEnd = null,
    )
}
