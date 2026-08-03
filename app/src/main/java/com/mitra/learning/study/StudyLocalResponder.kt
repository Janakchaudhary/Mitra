package com.mitra.learning.study

import com.mitra.learning.learning.evaluation.GujaratiNumberNormalizer
import com.mitra.learning.study.practice.EnglishSpellingLexicon

/**
 * Handles safe, deterministic Standard 2 maths and screen-time questions without a cloud model.
 * Textbook questions continue through StudyContextService + the configured AI provider.
 */
class StudyLocalResponder {
    fun respond(questionRaw: String): StudyAnswer? {
        val question = questionRaw.trim()
        if (question.isBlank()) return null

        if (isStopRequest(question)) {
            return StudyAnswer(
                answerGujarati = "બરાબર. હવે વાત અહીં બંધ કરીએ. ફરી શીખવું હોય ત્યારે મને બોલાવજો.",
                grounded = true,
                responseKind = StudyResponseKind.LOCAL_GUIDANCE,
                endConversation = true,
            )
        }

        if (isMobileGameQuestion(question)) {
            return StudyAnswer(
                answerGujarati = buildString {
                    append("અપણે હમણાં મિત્ર સાથે શીખવાની રમત જ રમી રહ્યા છીએ. ")
                    append("મોબાઇલમાં બહુ લાંબો સમય ગેમ રમીએ તો ભણવાનું, ઊંઘ, બહારની રમત અને પરિવાર સાથેનો સમય ઓછો થઈ શકે. ")
                    append("આપણે આ નાની શીખવાની રમત પૂરી કરીએ, પછી ફોનને આરામ આપીએ અને બહાર કે ઘરમાં કોઈ સાચી રમત રમીએ.")
                },
                followUpGujarati = "હવે ગણિત, જોડણી કે તમારા પુસ્તકમાંથી શું પૂછવું છે?",
                grounded = true,
                responseKind = StudyResponseKind.LOCAL_GUIDANCE,
            )
        }

        spellingReply(question)?.let { return it }
        conceptualMathReply(question)?.let { return it }
        parseArithmetic(question)?.let { arithmetic ->
            return StudyAnswer(
                answerGujarati = arithmetic.explanationGujarati,
                followUpGujarati = arithmetic.followUpGujarati,
                grounded = true,
                responseKind = StudyResponseKind.LOCAL_MATH,
            )
        }
        return null
    }

    fun isStopRequest(value: String): Boolean {
        val normalized = normalize(value)
        return STOP_PHRASES.any { normalized == " $it " || normalized.contains(" $it ") }
    }

    private fun isMobileGameQuestion(value: String): Boolean {
        val normalized = normalize(value)
        return GAME_WORDS.any { normalized.contains(" $it ") } ||
            (normalized.contains(" મોબાઇલ ") && normalized.contains(" રમત "))
    }


    private fun spellingReply(value: String): StudyAnswer? {
        val normalized = normalize(value)
        if (SPELLING_WORDS.none { normalized.contains(" $it ") }) return null
        val word = EnglishSpellingLexicon.find(value) ?: return StudyAnswer(
            answerGujarati = "કયો English શબ્દનો spelling જોઈએ છે? જેમ કે CAT, BOOK અથવા APPLE કહો.",
            followUpGujarati = "અથવા ‘મને spelling પૂછો’ કહો, તો હું voice spelling રમત શરૂ કરીશ.",
            grounded = true,
            responseKind = StudyResponseKind.LOCAL_GUIDANCE,
        )
        return StudyAnswer(
            answerGujarati = "${word.word.uppercase()} નો spelling ${EnglishSpellingLexicon.letters(word.word)} છે.",
            followUpGujarati = "હવે તમે અક્ષર-અક્ષર બોલી શકો?",
            grounded = true,
            responseKind = StudyResponseKind.LOCAL_GUIDANCE,
        )
    }

    private fun conceptualMathReply(value: String): StudyAnswer? {
        val normalized = normalize(value)
        val numbers = GujaratiNumberNormalizer.extractInts(value, maxCount = 2)

        if (CARRY_WORDS.any { normalized.contains(" $it ") } && numbers.size < 2) {
            return localMath(
                "કેરિ ત્યારે કરીએ જ્યારે એકમનો સરવાળો ૧૦ કે તેથી મોટો થાય. જેમ ૭ + ૮ = ૧૫: ૫ એકમ લખીએ અને ૧ દશક ઉપર લઈ જઈએ.",
                "હવે ૨૭ + ૧૮ માં એકમનો સરવાળો કેટલો થાય?",
            )
        }
        if (BORROW_WORDS.any { normalized.contains(" $it ") } && numbers.size < 2) {
            return localMath(
                "ઉધાર ત્યારે લઈએ જ્યારે ઉપરનો એકમ નાનો હોય. જેમ ૩૨ − ૧૭ માં ૨ માંથી ૭ ન કાઢી શકાય, એટલે ૧ દશક ઉધાર લઈ ૧૨ − ૭ કરીએ.",
                "ઉધાર લીધા પછી દશક કેટલું ઓછું થાય?",
            )
        }
        if (TABLE_WORDS.any { normalized.contains(" $it ") } && numbers.size == 1) {
            val table = numbers.first()
            if (table in 1..20) {
                val lines = (1..10).joinToString("\n") { multiplier ->
                    "$table × $multiplier = ${table * multiplier}"
                }
                return localMath(
                    "$table નો પહાડો ધીમે બોલીએ:\n$lines",
                    "હવે કહો: $table × 6 કેટલા?",
                )
            }
        }
        if (numbers.size == 2 && GREATER_WORDS.any { normalized.contains(" $it ") }) {
            val larger = maxOf(numbers[0], numbers[1])
            return localMath(
                "${numbers[0]} અને ${numbers[1]} માં $larger મોટી સંખ્યા છે. પહેલા દશક જુઓ; દશક સમાન હોય તો એકમ જુઓ.",
                "હવે નાની સંખ્યા કઈ છે?",
            )
        }
        if (numbers.size == 1 && AFTER_WORDS.any { normalized.contains(" $it ") }) {
            val number = numbers.first()
            return localMath(
                "$number પછી ${number + 1} આવે છે.",
                "અને $number પહેલાં કઈ સંખ્યા આવે?",
            )
        }
        if (numbers.size == 1 && BEFORE_WORDS.any { normalized.contains(" $it ") }) {
            val number = numbers.first()
            return localMath(
                "$number પહેલાં ${number - 1} આવે છે.",
                "અને $number પછી કઈ સંખ્યા આવે?",
            )
        }
        if (normalized.contains(" સરવાળો શું ") || normalized.contains(" addition ")) {
            return localMath(
                "સરવાળો એટલે બે કે વધુ સંખ્યાઓને ભેગી કરવી. ૨૩ + ૪ માં ૨૩ પછી ચાર આગળ ગણીએ તો ૨૭ થાય.",
                "તમારે એક નાનો સરવાળો પૂછવો છે?",
            )
        }
        if (normalized.contains(" બાદબાકી શું ") || normalized.contains(" subtraction ")) {
            return localMath(
                "બાદબાકી એટલે કુલમાંથી કંઈક કાઢવું અથવા બે સંખ્યાનો તફાવત શોધવો. ૧૫ − ૬ = ૯.",
                "હવે ૧૮ − ૭ કેટલા થાય?",
            )
        }
        return null
    }

    private fun parseArithmetic(value: String): ArithmeticReply? {
        val normalizedDigits = GujaratiNumberNormalizer.normalizeDigits(value.lowercase())
        val numbers = GujaratiNumberNormalizer.extractInts(value, maxCount = 2)
        if (numbers.size < 2) return null

        val padded = normalize(normalizedDigits)
        val operation = when {
            Regex("\\d\\s*[×*xX]\\s*\\d").containsMatchIn(normalizedDigits) ||
                MULTIPLY_WORDS.any { padded.contains(" $it ") } -> Operation.MULTIPLY
            normalizedDigits.contains('+') || ADD_WORDS.any { padded.contains(" $it ") } -> Operation.ADD
            normalizedDigits.contains('−') || normalizedDigits.contains('-') ||
                SUBTRACT_WORDS.any { padded.contains(" $it ") } -> Operation.SUBTRACT
            else -> null
        } ?: return null

        val a = numbers[0]
        val b = numbers[1]
        if (a !in 0..999 || b !in 0..999) return null

        return when (operation) {
            Operation.ADD -> additionReply(a, b)
            Operation.SUBTRACT -> subtractionReply(a, b)
            Operation.MULTIPLY -> multiplicationReply(a, b)
        }
    }

    private fun additionReply(a: Int, b: Int): ArithmeticReply {
        val answer = a + b
        val ones = a % 10 + b % 10
        val carry = ones / 10
        val onesDigit = ones % 10
        val tensTotal = a / 10 + b / 10 + carry
        val explanation = if (a < 100 && b < 100) {
            buildString {
                append("ચાલો $a + $b પગલાંથી કરીએ. ")
                append("પહેલા એકમ: ${a % 10} + ${b % 10} = $ones. ")
                if (carry > 0) {
                    append("$ones માંથી $onesDigit એકમ લખીએ અને ૧ દશક કેરી કરીએ. ")
                    append("હવે દશક: ${a / 10} + ${b / 10} + ૧ કેરી = $tensTotal. ")
                } else {
                    append("એટલે એકમમાં $onesDigit લખીએ. હવે દશક: ${a / 10} + ${b / 10} = $tensTotal. ")
                }
                append("જવાબ ${GujaratiNumberNormalizer.toGujaratiDigits(answer)} ($answer).")
            }
        } else {
            "$a અને $b ઉમેરતાં જવાબ ${GujaratiNumberNormalizer.toGujaratiDigits(answer)} ($answer) થાય. પહેલા એકમ, પછી દશક અને પછી સૈંકડો ઉમેરો."
        }
        return ArithmeticReply(explanation, "હવે તમે રફ કામમાં આ જ પગલાં બતાવી શકો?", answer)
    }

    private fun subtractionReply(a: Int, b: Int): ArithmeticReply {
        if (a < b) {
            return ArithmeticReply(
                "$a માંથી $b કાઢીએ તો જવાબ ઋણ સંખ્યા થાય. ધોરણ ૨ માટે પહેલા મોટી સંખ્યા ઉપર રાખીએ: $b − $a = ${b - a}.",
                "મોટી સંખ્યા કઈ છે?",
                a - b,
            )
        }
        val answer = a - b
        val borrow = a % 10 < b % 10
        val explanation = if (a < 100 && b < 100) {
            buildString {
                append("ચાલો $a − $b પગલાંથી કરીએ. ")
                if (borrow) {
                    append("એકમમાં ${a % 10} નાનું છે અને ${b % 10} કાઢવું છે, એટલે દશકમાંથી ૧ ઉધાર લઈએ. ")
                    append("એકમ હવે ${a % 10 + 10}; ${a % 10 + 10} − ${b % 10} = ${(a % 10 + 10) - b % 10}. ")
                    append("દશકમાં ${(a / 10) - 1} બાકી; ${(a / 10) - 1} − ${b / 10} = ${((a / 10) - 1) - b / 10}. ")
                } else {
                    append("એકમ: ${a % 10} − ${b % 10} = ${(a % 10) - b % 10}. ")
                    append("દશક: ${a / 10} − ${b / 10} = ${(a / 10) - b / 10}. ")
                }
                append("જવાબ ${GujaratiNumberNormalizer.toGujaratiDigits(answer)} ($answer).")
            }
        } else {
            "$a માંથી $b કાઢતાં જવાબ ${GujaratiNumberNormalizer.toGujaratiDigits(answer)} ($answer) થાય. એકમથી શરૂ કરીને જરૂર પડે ત્યારે ઉધાર લો."
        }
        return ArithmeticReply(explanation, "હવે એક વાર તમે રફ કામમાં કરીને પગલાં બોલો.", answer)
    }

    private fun multiplicationReply(a: Int, b: Int): ArithmeticReply {
        val answer = a * b
        val repeated = if (a in 1..10 && b in 1..10) List(a) { b }.joinToString(" + ") else null
        val explanation = buildString {
            append("$a × $b એટલે $a જૂથ, દરેક જૂથમાં $b. ")
            repeated?.let { append("એટલે $it = $answer. ") }
            append("જવાબ ${GujaratiNumberNormalizer.toGujaratiDigits(answer)} ($answer).")
        }
        return ArithmeticReply(explanation, "આને ઉલટું બોલો: $b × $a કેટલા?", answer)
    }

    private fun localMath(answer: String, followUp: String) = StudyAnswer(
        answerGujarati = answer,
        followUpGujarati = followUp,
        grounded = true,
        responseKind = StudyResponseKind.LOCAL_MATH,
    )

    private fun normalize(value: String): String = " " + value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim() + " "

    private enum class Operation { ADD, SUBTRACT, MULTIPLY }
    private data class ArithmeticReply(
        val explanationGujarati: String,
        val followUpGujarati: String,
        val answer: Int,
    )

    private companion object {
        val ADD_WORDS = setOf("વત્તા", "ઉમેર", "ઉમેરો", "સરવાળો", "plus", "add", "addition")
        val SUBTRACT_WORDS = setOf("બાદ", "ઓછા", "ઘટાડ", "ઘટાડો", "minus", "subtract", "subtraction")
        val MULTIPLY_WORDS = setOf("ગુણ્યા", "ગુણાકાર", "times", "multiply", "multiplication")
        val CARRY_WORDS = setOf("કેરી", "carry", "હાથમાં")
        val BORROW_WORDS = setOf("ઉધાર", "borrow", "borrowing")
        val TABLE_WORDS = setOf("પહાડો", "ટેબલ", "table", "tables", "ઘડિયા", "ઘડિયો", "ગડિયા")
        val GREATER_WORDS = setOf("મોટી", "મોટું", "મોટો", "greater", "larger", "big")
        val AFTER_WORDS = setOf("પછી", "પછીની", "આગળ", "after", "next")
        val BEFORE_WORDS = setOf("પહેલાં", "પહેલા", "પહેલાની", "પાછળ", "before", "previous")
        val SPELLING_WORDS = setOf("spelling", "સ્પેલિંગ", "જોડણી")
        val GAME_WORDS = setOf(
            "game", "games", "ગેમ", "ગેમ્સ", "બીજી ગેમ", "મોબાઇલ ગેમ",
            "pubg", "free fire", "roblox", "minecraft", "bgmi",
        )
        val STOP_PHRASES = setOf("બસ", "બંધ", "રોકો", "stop", "bye", "આવજો")
    }
}
