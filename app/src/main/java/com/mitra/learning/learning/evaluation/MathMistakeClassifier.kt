package com.mitra.learning.learning.evaluation

import com.mitra.learning.learning.model.ArithmeticWork

enum class MathMistakeCode {
    FORGOT_CARRY,
    FORGOT_BORROW,
    ONES_COLUMN,
    TENS_COLUMN,
    REVERSED_DIGITS,
    MULTIPLICATION_FACT,
    GENERAL,
}

data class MathMistake(
    val code: MathMistakeCode,
    val hintGujarati: String,
)

/** Classifies common Standard 2 arithmetic mistakes without AI. */
object MathMistakeClassifier {
    fun classify(work: ArithmeticWork, childAnswer: Int, expected: Int): MathMistake {
        if (reverseDigits(expected) == childAnswer && expected >= 10) {
            return MathMistake(
                MathMistakeCode.REVERSED_DIGITS,
                "દશક અને એકમની જગ્યા ઉલટી થઈ ગઈ છે. જવાબના અંકો યોગ્ય ખાને ફરી લખો.",
            )
        }

        return when (work.operator) {
            "+" -> classifyAddition(work, childAnswer, expected)
            "−", "-" -> classifySubtraction(work, childAnswer, expected)
            "×", "x", "*" -> MathMistake(
                MathMistakeCode.MULTIPLICATION_FACT,
                "જૂથ બનાવીને ગણો અથવા પહાડો ધીમે બોલો. ${work.top} જૂથમાં ${work.bottom} વસ્તુ વિચારો.",
            )
            else -> MathMistake(MathMistakeCode.GENERAL, "રફ કામમાં એકમથી ફરી શરૂ કરો.")
        }
    }

    private fun classifyAddition(work: ArithmeticWork, child: Int, expected: Int): MathMistake {
        val onesTotal = work.top % 10 + work.bottom % 10
        val noCarry = (work.top / 10 + work.bottom / 10) * 10 + (onesTotal % 10)
        if (onesTotal >= 10 && child == noCarry) {
            return MathMistake(
                MathMistakeCode.FORGOT_CARRY,
                "એકમનો સરવાળો $onesTotal થાય છે. ${onesTotal % 10} એકમ લખો અને ૧ દશક ઉપર કેરી કરો.",
            )
        }
        if (child % 10 != expected % 10) {
            return MathMistake(
                MathMistakeCode.ONES_COLUMN,
                "પહેલા એકમ તપાસો: ${work.top % 10} + ${work.bottom % 10} કેટલા?",
            )
        }
        if (child / 10 != expected / 10) {
            return MathMistake(
                MathMistakeCode.TENS_COLUMN,
                if (onesTotal >= 10) "દશક ઉમેરતી વખતે ઉપરની ૧ કેરી ભૂલશો નહીં."
                else "હવે દશકનો સરવાળો ફરી કરો.",
            )
        }
        return MathMistake(MathMistakeCode.GENERAL, "એકમ અને દશક બંને ખાના ફરી તપાસો.")
    }

    private fun classifySubtraction(work: ArithmeticWork, child: Int, expected: Int): MathMistake {
        val needsBorrow = work.top % 10 < work.bottom % 10
        val largerDigitMethod = kotlin.math.abs(work.top / 10 - work.bottom / 10) * 10 +
            kotlin.math.abs(work.top % 10 - work.bottom % 10)
        if (needsBorrow && child == largerDigitMethod) {
            return MathMistake(
                MathMistakeCode.FORGOT_BORROW,
                "એકમમાં ${work.top % 10} માંથી ${work.bottom % 10} સીધું કાઢી શકાતું નથી. દશકમાંથી ૧ ઉધાર લો.",
            )
        }
        if (child % 10 != expected % 10) {
            return MathMistake(
                MathMistakeCode.ONES_COLUMN,
                if (needsBorrow) "ઉધાર લીધા પછી એકમ ${work.top % 10 + 10} થાય છે. હવે ${work.bottom % 10} કાઢો."
                else "એકમનો બાદબાકી ભાગ ફરી કરો.",
            )
        }
        if (child / 10 != expected / 10) {
            return MathMistake(
                MathMistakeCode.TENS_COLUMN,
                if (needsBorrow) "દશકમાંથી ૧ ઉધાર આપ્યા પછી દશક એક ઓછું રહે છે."
                else "દશકનો બાદબાકી ભાગ ફરી કરો.",
            )
        }
        return MathMistake(MathMistakeCode.GENERAL, "ઉપર મોટી સંખ્યા રાખીને એકમથી ફરી ગણો.")
    }

    private fun reverseDigits(value: Int): Int = value.toString().reversed().toIntOrNull() ?: value
}
