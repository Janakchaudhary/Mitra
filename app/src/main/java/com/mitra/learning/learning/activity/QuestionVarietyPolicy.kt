package com.mitra.learning.learning.activity

import com.mitra.learning.learning.model.LearningQuestion

/**
 * Removes duplicates inside a session, prefers questions not used recently, and interleaves
 * activity types when the generator supplied more than one type.
 */
object QuestionVarietyPolicy {
    fun select(
        generated: List<LearningQuestion>,
        targetCount: Int,
        recentFingerprints: Set<String>,
    ): List<LearningQuestion> {
        val target = targetCount.coerceAtLeast(1)
        val unique = generated.distinctBy { it.fingerprint }
        val prioritized = unique.filterNot { it.fingerprint in recentFingerprints } +
            unique.filter { it.fingerprint in recentFingerprints }
        val remaining = prioritized.toMutableList()
        val selected = mutableListOf<LearningQuestion>()
        while (remaining.isNotEmpty() && selected.size < target) {
            val lastType = selected.lastOrNull()?.activityType
            val nextIndex = remaining.indexOfFirst { it.activityType != lastType }.takeIf { it >= 0 } ?: 0
            selected += remaining.removeAt(nextIndex)
        }
        return selected
    }
}
