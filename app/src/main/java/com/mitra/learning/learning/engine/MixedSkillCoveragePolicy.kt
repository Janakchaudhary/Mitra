package com.mitra.learning.learning.engine

import com.mitra.learning.learning.model.LearningQuestion

/**
 * Keeps the adaptive order while enforcing the promises made by mixed-skill mode:
 * - include at least one carry/borrow question when the pool contains one;
 * - expose several different concepts instead of selecting only high-variety activity types;
 * - preserve unique fingerprints and the requested session size.
 */
object MixedSkillCoveragePolicy {
    fun apply(
        selected: List<LearningQuestion>,
        candidates: List<LearningQuestion>,
        count: Int,
        minimumDistinctConcepts: Int = 4,
    ): List<LearningQuestion> {
        val target = count.coerceAtLeast(0)
        if (target == 0) return emptyList()

        val pool = candidates.distinctBy { it.fingerprint }
        val result = selected.distinctBy { it.fingerprint }.take(target).toMutableList()
        pool.asSequence()
            .filterNot { candidate -> result.any { it.fingerprint == candidate.fingerprint } }
            .take((target - result.size).coerceAtLeast(0))
            .forEach(result::add)

        if (result.isEmpty()) return result

        val carryCandidate = pool.firstOrNull { it.arithmeticWork?.regrouping == true }
        if (carryCandidate != null && result.none { it.arithmeticWork?.regrouping == true }) {
            replaceForCoverage(result, carryCandidate)
        }

        val availableConcepts = pool.mapNotNull { it.conceptId }.distinct()
        val requiredConcepts = minOf(minimumDistinctConcepts, target, availableConcepts.size)
        while (result.mapNotNull { it.conceptId }.distinct().size < requiredConcepts) {
            val selectedConcepts = result.mapNotNull { it.conceptId }.toSet()
            val missingConceptQuestion = pool.firstOrNull { candidate ->
                candidate.conceptId != null &&
                    candidate.conceptId !in selectedConcepts &&
                    result.none { it.fingerprint == candidate.fingerprint }
            } ?: break
            if (!replaceForCoverage(result, missingConceptQuestion)) break
        }

        return result.distinctBy { it.fingerprint }.take(target)
    }

    private fun replaceForCoverage(
        result: MutableList<LearningQuestion>,
        replacement: LearningQuestion,
    ): Boolean {
        if (result.any { it.fingerprint == replacement.fingerprint }) return true

        val conceptCounts = result.mapNotNull { it.conceptId }.groupingBy { it }.eachCount()
        val regroupingCount = result.count { it.arithmeticWork?.regrouping == true }

        // Keep the fourth slot available for ActivityPlanPolicy's off-screen break when possible.
        val offScreenSlot = minOf(3, result.lastIndex)
        val replacementOrder = result.indices.reversed().filter { it != offScreenSlot } + offScreenSlot
        val replaceIndex = replacementOrder.firstOrNull { index ->
            val current = result[index]
            val preservesOnlyRegrouping = current.arithmeticWork?.regrouping == true && regroupingCount <= 1
            val preservesConceptDiversity = current.conceptId == null || (conceptCounts[current.conceptId] ?: 0) > 1
            !preservesOnlyRegrouping && preservesConceptDiversity
        } ?: replacementOrder.firstOrNull { index ->
            val current = result[index]
            current.arithmeticWork?.regrouping != true || regroupingCount > 1
        } ?: return false

        result[replaceIndex] = replacement
        return true
    }
}
