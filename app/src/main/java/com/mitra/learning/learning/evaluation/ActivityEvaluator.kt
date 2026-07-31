package com.mitra.learning.learning.evaluation

import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion

object ActivityEvaluator {
    fun evaluate(activity: LearningQuestion, answerText: String): AttemptResult {
        return when (activity.evaluationMode) {
            EvaluationMode.NUMERIC -> {
                val expected = activity.expectedAnswer ?: return AttemptResult.UNKNOWN
                val parsed = GujaratiNumberNormalizer.parseInt(answerText)
                if (parsed == expected) AttemptResult.CORRECT else AttemptResult.INCORRECT
            }
            EvaluationMode.MULTIPLE_CHOICE,
            EvaluationMode.SHORT_TEXT -> {
                val answer = normalize(answerText)
                val accepted = accepted(activity)
                if (answer.isNotBlank() && answer in accepted) AttemptResult.CORRECT else AttemptResult.INCORRECT
            }
            EvaluationMode.KEYWORD -> {
                val answer = normalize(answerText)
                val accepted = accepted(activity)
                if (answer.isNotBlank() && accepted.any { it.isNotBlank() && answer.contains(it) }) {
                    AttemptResult.CORRECT
                } else {
                    AttemptResult.INCORRECT
                }
            }
            EvaluationMode.PARTICIPATION -> AttemptResult.UNKNOWN
        }
    }

    private fun accepted(activity: LearningQuestion): Set<String> = buildSet {
        activity.expectedText?.let { add(normalize(it)) }
        activity.acceptedAnswers.forEach { add(normalize(it)) }
    }.filter { it.isNotBlank() }.toSet()

    internal fun normalize(value: String): String = value
        .trim()
        .lowercase()
        .replace(Regex("[\\p{Punct}।॥]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
