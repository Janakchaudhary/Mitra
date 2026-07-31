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
            val index = minOf(3, sanitized.lastIndex)
            sanitized[index] = ActivitySafetyPolicy.sanitize(
                LearningQuestion(
                    id = "local-offscreen-${sanitized[index].id}",
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
