package com.mitra.learning.study.practice

object MitraPracticeEvaluator {
    fun evaluate(
        challenge: MitraVoiceChallenge,
        rawAnswer: String,
        correctStreak: Int,
    ): MitraPracticeEvaluation {
        val expectedTexts = buildSet {
            challenge.expectedText?.let { add(normalizeComparable(it, challenge.kind)) }
            challenge.acceptedAnswers.forEach { add(normalizeComparable(it, challenge.kind)) }
        }.filter(String::isNotBlank).toSet()

        val normalizedAnswer: String
        val correct = when (challenge.evaluationMode) {
            MitraChallengeEvaluationMode.NUMERIC -> {
                val parsed = SpokenAnswerNormalizer.numeric(rawAnswer)
                normalizedAnswer = parsed?.toString().orEmpty()
                parsed != null && parsed == challenge.expectedNumber
            }
            MitraChallengeEvaluationMode.EXACT_TEXT -> {
                normalizedAnswer = normalizeComparable(rawAnswer, challenge.kind)
                normalizedAnswer.isNotBlank() && normalizedAnswer in expectedTexts
            }
            MitraChallengeEvaluationMode.KEYWORD -> {
                normalizedAnswer = normalizeComparable(rawAnswer, challenge.kind)
                normalizedAnswer.isNotBlank() && expectedTexts.any { expected ->
                    expected.isNotBlank() &&
                        (normalizedAnswer.contains(expected) || expected.contains(normalizedAnswer))
                }
            }
        }

        val feedback = if (correct) {
            appreciation(challenge, correctStreak + 1)
        } else {
            challenge.hintGujarati?.takeIf(String::isNotBlank)
                ?.let { "હજી થોડું વિચારીએ. સંકેત: $it" }
                ?: "હજી સાચું નથી થયું. ધીમે વિચારીએ."
        }
        return MitraPracticeEvaluation(correct, feedback, normalizedAnswer)
    }

    private fun appreciation(challenge: MitraVoiceChallenge, streak: Int): String {
        val base = PRAISE[Math.floorMod(challenge.id.hashCode() + streak, PRAISE.size)]
        return when {
            streak >= 5 -> "$base સતત $streak સાચા જવાબ! તમે અદ્ભુત ધ્યાનથી શીખી રહ્યા છો."
            streak >= 3 -> "$base આ સતત $streak મો સાચો જવાબ છે!"
            else -> "$base સાચો જવાબ."
        }
    }

    private fun normalizeComparable(value: String, kind: MitraChallengeKind): String = when (kind) {
        MitraChallengeKind.SPELLING -> SpokenAnswerNormalizer.spelling(value)
        else -> SpokenAnswerNormalizer.text(value)
    }

    private val PRAISE = listOf(
        "વાહ! બહુ સરસ!",
        "શાબાશ!",
        "એકદમ સાચું!",
        "કમાલ કરી!",
        "સરસ વિચાર્યું!",
        "હા, બરાબર!",
    )
}
