package com.mitra.learning.learning.assignment

import com.mitra.learning.learning.model.LearningQuestion

/**
 * Selects a deterministic, varied set of unique questions from one prepared chapter.
 * It balances activity type, source page and concept so a focused test does not become
 * twenty near-identical vocabulary or first-page questions.
 */
object FocusedChapterQuestionSelector {
    fun select(candidates: List<LearningQuestion>, count: Int): List<LearningQuestion> {
        val remaining = candidates
            .filter { it.requiresAnswer }
            .distinctBy { it.fingerprint }
            .sortedWith(
                compareBy<LearningQuestion> { it.sourcePage ?: Int.MAX_VALUE }
                    .thenBy { activityPriority(it.activityType) }
                    .thenBy { it.fingerprint }
            )
            .toMutableList()
        if (count <= 0 || remaining.isEmpty()) return emptyList()

        val selected = mutableListOf<LearningQuestion>()
        val typeUse = mutableMapOf<String, Int>()
        val pageUse = mutableMapOf<Int, Int>()
        val conceptUse = mutableMapOf<String, Int>()

        while (selected.size < count && remaining.isNotEmpty()) {
            val previous = selected.lastOrNull()
            val best = remaining.maxWithOrNull(
                compareBy<LearningQuestion> { question ->
                    val typeCount = typeUse[question.activityType].orZero()
                    val pageCount = question.sourcePage?.let { pageUse[it].orZero() } ?: 0
                    val conceptCount = question.conceptId?.let { conceptUse[it].orZero() } ?: 0
                    var score = 10_000
                    score -= typeCount * 900
                    score -= pageCount * 500
                    score -= conceptCount * 350
                    if (previous?.activityType == question.activityType) score -= 450
                    if (previous?.sourcePage != null && previous.sourcePage == question.sourcePage) score -= 300
                    score - activityPriority(question.activityType) * 5
                }.thenByDescending { it.fingerprint }
            ) ?: break

            selected += best
            remaining.remove(best)
            typeUse[best.activityType] = typeUse[best.activityType].orZero() + 1
            best.sourcePage?.let { pageUse[it] = pageUse[it].orZero() + 1 }
            best.conceptId?.let { conceptUse[it] = conceptUse[it].orZero() + 1 }
        }
        return selected
    }

    private fun activityPriority(activityType: String): Int = when (activityType.uppercase()) {
        "VOCABULARY", "SPELLING", "MISSING_LETTER" -> 0
        "QUESTION", "BOOK_LOOK", "READING" -> 1
        "MULTIPLE_CHOICE" -> 2
        "RIDDLE", "WORD_PROBLEM", "TEACH_MITRA" -> 3
        else -> 4
    }

    private fun Int?.orZero(): Int = this ?: 0
}
