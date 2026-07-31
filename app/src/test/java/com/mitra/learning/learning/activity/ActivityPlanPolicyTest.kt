package com.mitra.learning.learning.activity

import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.LearningQuestion
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPlanPolicyTest {
    @Test
    fun fourScreenActivitiesGainAnOffScreenMission() {
        val plan = ActivityPlanPolicy.apply(
            List(4) { index ->
                LearningQuestion(
                    id = "q$index",
                    promptGujarati = "પ્રશ્ન $index",
                    expectedAnswer = index,
                )
            }
        )

        assertTrue(plan.any { it.type == ActivityType.PHYSICAL_MISSION })
    }
}
