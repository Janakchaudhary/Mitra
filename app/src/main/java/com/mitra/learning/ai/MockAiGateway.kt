package com.mitra.learning.ai

import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.curriculum.BuiltInCurriculum
import com.mitra.learning.learning.model.LearningQuestion

class MockAiGateway : AiGateway {
    override suspend fun createPracticeQuestions(
        concept: ConceptEntity,
        count: Int,
    ): List<LearningQuestion> {
        val base = when (concept.id) {
            BuiltInCurriculum.COUNT_1_20 -> listOf(
                q("c1", "૪ પછી કયો અંક આવે?", 5),
                q("c2", "૯ પછી કયો અંક આવે?", 10),
                q("c3", "૧૪ પછી કયો અંક આવે?", 15),
                q("c4", "૮ પહેલાં કયો અંક આવે?", 7),
                q("c5", "૧૯ પછી કયો અંક આવે?", 20),
            )
            BuiltInCurriculum.COUNT_21_50 -> listOf(
                q("c21", "૨૯ પછી કયો અંક આવે?", 30),
                q("c22", "૩૪ પહેલાં કયો અંક આવે?", 33),
                q("c23", "૪૧ પછી કયો અંક આવે?", 42),
                q("c24", "૨૫ પછી બે અંક ગણો. કયો અંક આવશે?", 27),
                q("c25", "૪૯ પછી કયો અંક આવે?", 50),
            )
            BuiltInCurriculum.ADD_UNDER_10 -> listOf(
                q("a1", "તમારી પાસે ૩ કેરી છે. ૨ વધુ મળે તો કુલ કેટલી?", 5),
                q("a2", "૪ + ૩ કેટલા?", 7),
                q("a3", "૨ પેન્સિલ અને ૬ પેન્સિલ મળીને કેટલી?", 8),
                q("a4", "૫ + ૪ કેટલા?", 9),
                q("a5", "૧ + ૬ કેટલા?", 7),
            )
            BuiltInCurriculum.ADD_UNDER_20 -> listOf(
                q("a21", "૮ + ૫ કેટલા?", 13),
                q("a22", "૯ + ૭ કેટલા?", 16),
                q("a23", "૧૧ + ૪ કેટલા?", 15),
                q("a24", "૬ + ૧૨ કેટલા?", 18),
                q("a25", "૧૦ + ૯ કેટલા?", 19),
            )
            BuiltInCurriculum.SUBTRACT_UNDER_10 -> listOf(
                q("s1", "તમારી પાસે ૭ લાડુ છે. ૨ આપી દો તો કેટલા રહે?", 5),
                q("s2", "૯ - ૩ કેટલા?", 6),
                q("s3", "૮ માંથી ૫ કાઢો. કેટલા રહે?", 3),
                q("s4", "૬ - ૧ કેટલા?", 5),
                q("s5", "૧૦ માંથી ૪ કાઢો. કેટલા રહે?", 6),
            )
            else -> listOf(q("fallback", "૨ + ૨ કેટલા?", 4))
        }
        if (count <= base.size) return base.take(count)
        return List(count) { index -> base[index % base.size].copy(id = "${base[index % base.size].id}-$index") }
    }

    override fun feedbackGujarati(result: AttemptResult, expectedAnswer: Int): String = when (result) {
        AttemptResult.CORRECT -> "હા! સાચું. તમે કેવી રીતે શોધ્યું તે યાદ રાખજો."
        AttemptResult.INCORRECT -> "ફરી વિચારીએ. વસ્તુઓ ગણીને અથવા આંગળીઓથી અજમાવો. સાચો જવાબ $expectedAnswer છે."
        AttemptResult.PARTIAL -> "લગભગ સાચું. એક વાર ફરી ધીમે વિચારીએ."
        AttemptResult.SKIPPED -> "ઠીક છે. આ પ્રશ્ન પછી ફરી અજમાવીશું."
        AttemptResult.UNKNOWN -> "ચાલો આ પ્રશ્ન ફરીથી અજમાવીએ."
    }

    private fun q(id: String, text: String, answer: Int) = LearningQuestion(
        id = id,
        promptGujarati = text,
        expectedAnswer = answer,
    )
}
