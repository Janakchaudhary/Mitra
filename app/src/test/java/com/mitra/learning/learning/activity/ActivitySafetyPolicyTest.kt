package com.mitra.learning.learning.activity

import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ActivitySafetyPolicyTest {
    @Test
    fun remotePhysicalInstructionIsReplacedWithLocalSafeMission() {
        val unsafe = LearningQuestion(
            id = "mission-1",
            promptGujarati = "બહાર જઈ રસ્તો પાર કરો",
            activityType = ActivityType.PHYSICAL_MISSION.name,
            evaluationMode = EvaluationMode.PARTICIPATION,
        )

        val safe = ActivitySafetyPolicy.sanitize(unsafe)

        assertFalse(safe.promptGujarati.contains("રસ્તો પાર"))
        assertEquals(EvaluationMode.PARTICIPATION, safe.evaluationMode)
        assertEquals("મિશન પૂરું", safe.completionButtonGujarati)
    }
}
