package com.mitra.learning.learning.engine

import com.mitra.learning.data.db.entity.AttemptEntity
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.LearningQuestion

/**
 * Orders a candidate pool using evidence about the exact question/fact, not only broad concept
 * mastery. Incorrect or skipped facts return sooner, while recently-correct facts are spaced out.
 * The planner also limits runs of the same activity type so a 20/25-question session feels varied.
 */
object AdaptiveSessionPlanner {
    fun select(
        candidates: List<LearningQuestion>,
        recentAttempts: List<AttemptEntity>,
        count: Int,
        recentFingerprints: Set<String> = emptySet(),
        nowMillis: Long = System.currentTimeMillis(),
    ): List<LearningQuestion> {
        val target = count.coerceAtLeast(0)
        if (target == 0) return emptyList()
        val unique = candidates.distinctBy { it.fingerprint }
        if (unique.size <= 1) return unique.take(target)

        val evidence = recentAttempts
            .filter { it.questionFingerprint.isNotBlank() }
            .groupBy { it.questionFingerprint }
            .mapValues { (_, attempts) ->
                FactEvidence(
                    incorrect = attempts.count { it.result == AttemptResult.INCORRECT || it.result == AttemptResult.PARTIAL },
                    skipped = attempts.count { it.result == AttemptResult.SKIPPED },
                    correct = attempts.count { it.result == AttemptResult.CORRECT },
                    lastAttemptAt = attempts.maxOfOrNull { it.createdAt },
                )
            }

        val remaining = unique.toMutableList()
        val selected = mutableListOf<LearningQuestion>()
        while (remaining.isNotEmpty() && selected.size < target) {
            val recentTypes = selected.takeLast(2).map { it.type }
            val best = remaining.maxWithOrNull(
                compareBy<LearningQuestion> { question ->
                    score(
                        question = question,
                        evidence = evidence[question.fingerprint],
                        alreadyRecent = question.fingerprint in recentFingerprints,
                        repeatedType = recentTypes.size == 2 && recentTypes.all { it == question.type },
                        nowMillis = nowMillis,
                    )
                }.thenByDescending { it.sourcePage ?: Int.MIN_VALUE }
                    .thenByDescending { it.fingerprint }
            ) ?: break
            selected += best
            remaining.remove(best)
        }
        return selected
    }

    private fun score(
        question: LearningQuestion,
        evidence: FactEvidence?,
        alreadyRecent: Boolean,
        repeatedType: Boolean,
        nowMillis: Long,
    ): Int {
        var score = when {
            evidence == null -> 35 // a new fact deserves some exposure
            else -> evidence.incorrect * 22 + evidence.skipped * 12 - evidence.correct * 5
        }
        if (alreadyRecent) score -= 24
        if (repeatedType) score -= 18
        if (question.type in VARIETY_BONUS_TYPES) score += 3
        evidence?.lastAttemptAt?.let { last ->
            val ageDays = ((nowMillis - last).coerceAtLeast(0L) / DAY_MS).toInt()
            score += ageDays.coerceAtMost(14)
            if (ageDays == 0 && evidence.correct > evidence.incorrect) score -= 10
        }
        return score
    }

    private data class FactEvidence(
        val incorrect: Int,
        val skipped: Int,
        val correct: Int,
        val lastAttemptAt: Long?,
    )

    private val VARIETY_BONUS_TYPES = setOf(
        ActivityType.WORD_PROBLEM,
        ActivityType.VOCABULARY,
        ActivityType.SPELLING,
        ActivityType.MULTIPLE_CHOICE,
        ActivityType.BOOK_LOOK,
    )
    private const val DAY_MS = 86_400_000L
}
