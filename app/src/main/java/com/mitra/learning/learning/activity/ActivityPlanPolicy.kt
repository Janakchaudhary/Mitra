package com.mitra.learning.learning.activity

import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion

object ActivityPlanPolicy {
    private val offScreenTypes = setOf(
        ActivityType.PHYSICAL_MISSION,
        ActivityType.BOOK_LOOK,
        ActivityType.DRAW,
    )

    fun apply(input: List<LearningQuestion>): List<LearningQuestion> {
        if (input.isEmpty()) return input
        val sanitized = input.map(ActivitySafetyPolicy::sanitize).toMutableList()

        if (sanitized.size >= 4 && sanitized.none { it.type in offScreenTypes }) {
            val conceptCounts = sanitized.mapNotNull { it.conceptId }.groupingBy { it }.eachCount()
            val regroupingCount = sanitized.count { it.arithmeticWork?.regrouping == true }
            val desiredIndex = minOf(3, sanitized.lastIndex)

            fun canReplace(index: Int, requireDuplicateConcept: Boolean): Boolean {
                val question = sanitized[index]
                val keepsRegrouping = question.arithmeticWork?.regrouping != true || regroupingCount > 1
                val replacesDuplicateConcept = question.conceptId == null ||
                    (conceptCounts[question.conceptId] ?: 0) > 1
                return keepsRegrouping && (!requireDuplicateConcept || replacesDuplicateConcept)
            }

            val searchOrder = listOf(desiredIndex) +
                sanitized.indices.filter { it != desiredIndex }.sortedBy { kotlin.math.abs(it - desiredIndex) }
            val replacementIndex = searchOrder.firstOrNull { canReplace(it, requireDuplicateConcept = true) }
                ?: searchOrder.firstOrNull { canReplace(it, requireDuplicateConcept = false) }
                ?: desiredIndex

            // Keep the movement break around the fourth activity while moving the displaced
            // learning question into the safe replacement slot.
            if (replacementIndex != desiredIndex) {
                sanitized[replacementIndex] = sanitized[desiredIndex]
            }
            sanitized[desiredIndex] = ActivitySafetyPolicy.sanitize(
                LearningQuestion(
                    id = "local-offscreen-${sanitized[desiredIndex].id}",
                    promptGujarati = "નાનું સુરક્ષિત મિશન",
                    activityType = ActivityType.PHYSICAL_MISSION.name,
                    evaluationMode = EvaluationMode.PARTICIPATION,
                    completionButtonGujarati = "મિશન પૂરું",
                )
            )
        }

        return sanitized
    }
}
