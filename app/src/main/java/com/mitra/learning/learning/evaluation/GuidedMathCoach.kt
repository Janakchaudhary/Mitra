package com.mitra.learning.learning.evaluation

import com.mitra.learning.learning.model.ArithmeticWork

enum class GuidedMathStep { ONES, REGROUP, TENS, COMPLETE }

data class GuidedMathExpected(
    val ones: Int,
    val regroup: Int,
    val tens: Int,
    val regroupLabelGujarati: String,
)

data class GuidedMathStepFeedback(
    val correct: Boolean,
    val step: GuidedMathStep,
    val messageGujarati: String,
)

/** Local, step-by-step checker for two-digit column arithmetic. */
object GuidedMathCoach {
    fun expected(work: ArithmeticWork): GuidedMathExpected? {
        return when (work.operator) {
        "+" -> {
            val onesTotal = work.top % 10 + work.bottom % 10
            val carry = onesTotal / 10
            GuidedMathExpected(
                ones = onesTotal % 10,
                regroup = carry,
                tens = work.top / 10 + work.bottom / 10 + carry,
                regroupLabelGujarati = "કેરી",
            )
        }
        "−", "-" -> {
            if (work.top < work.bottom) return null
            val borrow = if (work.top % 10 < work.bottom % 10) 1 else 0
            GuidedMathExpected(
                ones = work.top % 10 + borrow * 10 - work.bottom % 10,
                regroup = borrow,
                tens = work.top / 10 - work.bottom / 10 - borrow,
                regroupLabelGujarati = "ઉધાર",
            )
        }
            else -> null
        }
    }

    fun check(
        work: ArithmeticWork,
        ones: Int?,
        regroup: Int?,
        tens: Int?,
    ): GuidedMathStepFeedback {
        val expected = expected(work)
            ?: return GuidedMathStepFeedback(false, GuidedMathStep.ONES, "આ માર્ગદર્શન બે અંકના સરવાળા અથવા બાદબાકી માટે છે.")

        if (ones == null) {
            return GuidedMathStepFeedback(false, GuidedMathStep.ONES, "પહેલા એકમના ખાનાનો જવાબ લખો.")
        }
        if (ones != expected.ones) {
            val symbol = if (work.operator == "+") "+" else "−"
            val topOnes = work.top % 10 + if (work.operator != "+" && expected.regroup == 1) 10 else 0
            return GuidedMathStepFeedback(
                false,
                GuidedMathStep.ONES,
                "એકમ ફરી જુઓ: $topOnes $symbol ${work.bottom % 10} કેટલા?",
            )
        }

        if (work.regrouping || expected.regroup > 0) {
            if (regroup == null) {
                return GuidedMathStepFeedback(
                    false,
                    GuidedMathStep.REGROUP,
                    "હવે ${expected.regroupLabelGujarati}ના ખાનામાં ૦ અથવા ૧ લખો.",
                )
            }
            if (regroup != expected.regroup) {
                return GuidedMathStepFeedback(
                    false,
                    GuidedMathStep.REGROUP,
                    if (expected.regroup == 1) "અહીં ૧ ${expected.regroupLabelGujarati} જરૂરી છે."
                    else "અહીં ${expected.regroupLabelGujarati} કરવાની જરૂર નથી; ૦ લખો.",
                )
            }
        }

        if (tens == null) {
            return GuidedMathStepFeedback(false, GuidedMathStep.TENS, "હવે દશકના ખાનાનો જવાબ લખો.")
        }
        if (tens != expected.tens) {
            return GuidedMathStepFeedback(
                false,
                GuidedMathStep.TENS,
                if (expected.regroup == 1 && work.operator == "+") "દશક ગણતી વખતે ઉપરની ૧ કેરી ઉમેરો."
                else if (expected.regroup == 1) "દશકમાંથી ૧ ઉધાર આપ્યું હતું, એટલે ઉપરનો દશક એક ઓછો છે."
                else "દશકનો ભાગ ફરી ગણો.",
            )
        }

        return GuidedMathStepFeedback(true, GuidedMathStep.COMPLETE, "બધા પગલાં સાચા! હવે અંતિમ જવાબ લખો.")
    }
}
