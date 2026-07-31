package com.mitra.learning.learning.activity

import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion

/**
 * Child-facing physical activity text is never accepted from a remote model verbatim.
 * We keep the activity kind, but replace the instruction with a small local allowlist.
 */
object ActivitySafetyPolicy {
    fun sanitize(activity: LearningQuestion): LearningQuestion {
        return when (activity.type) {
            ActivityType.PHYSICAL_MISSION -> activity.copy(
                promptGujarati = safePhysicalPrompt(activity.id),
                evaluationMode = EvaluationMode.PARTICIPATION,
                expectedAnswer = null,
                expectedText = null,
                acceptedAnswers = emptyList(),
                optionsGujarati = emptyList(),
                completionButtonGujarati = "મિશન પૂરું",
            )
            ActivityType.DRAW -> activity.copy(
                promptGujarati = safeDrawPrompt(activity.promptGujarati),
                evaluationMode = EvaluationMode.PARTICIPATION,
                expectedAnswer = null,
                expectedText = null,
                acceptedAnswers = emptyList(),
                optionsGujarati = emptyList(),
                completionButtonGujarati = "દોરી લીધું",
            )
            ActivityType.TEACH_MITRA -> activity.copy(
                evaluationMode = EvaluationMode.PARTICIPATION,
                expectedAnswer = null,
                expectedText = null,
                acceptedAnswers = emptyList(),
                optionsGujarati = emptyList(),
                completionButtonGujarati = activity.completionButtonGujarati.ifBlank { "સમજાવી દીધું" },
            )
            ActivityType.BOOK_LOOK,
            ActivityType.STORY,
            ActivityType.RECAP -> activity.copy(
                evaluationMode = if (activity.requiresAnswer) activity.evaluationMode else EvaluationMode.PARTICIPATION,
                completionButtonGujarati = activity.completionButtonGujarati.ifBlank { "થઈ ગયું" },
            )
            else -> activity
        }
    }

    private fun safePhysicalPrompt(id: String): String {
        val prompts = listOf(
            "ફોનને નીચે મૂકો. તમારા નજીક ૩ પેન્સિલ અથવા ક્રેયોન શોધો. મળી જાય પછી પાછા આવી ‘મિશન પૂરું’ દબાવો.",
            "તમારા રૂમમાં સુરક્ષિત રીતે ૨ ગોળ વસ્તુ શોધો. વસ્તુ ઉપાડવી જરૂરી નથી. પછી ‘મિશન પૂરું’ દબાવો.",
            "પુસ્તક અને પેન્સિલ તમારી સામે મૂકો. બંને તૈયાર થાય પછી ‘મિશન પૂરું’ દબાવો.",
            "તમારી જગ્યાએ ઉભા રહી ૫ વાર તાળી પાડો. પછી ‘મિશન પૂરું’ દબાવો.",
        )
        return prompts[(id.hashCode() and Int.MAX_VALUE) % prompts.size]
    }

    private fun safeDrawPrompt(@Suppress("UNUSED_PARAMETER") original: String): String {
        // Remote text is deliberately not echoed into a physical instruction.
        // The child only receives this local, stationary paper-and-pencil activity.
        return "કાગળ અને પેન્સિલ લો. આપણે જે પાઠ શીખી રહ્યા છીએ તેનો એક સરળ વિચાર અથવા ચિત્ર દોરો. પછી ‘દોરી લીધું’ દબાવો."
    }
}
