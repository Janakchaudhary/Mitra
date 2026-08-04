package com.mitra.learning.learning.curriculum

import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.activity.QuestionVarietyPolicy
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.ArithmeticWork
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import kotlin.random.Random

/** Local, varied Standard 2 drills. No network or LLM is required. */
object Standard2SkillActivityFactory {
    fun create(
        concept: ConceptEntity,
        count: Int,
        seed: Long = System.nanoTime(),
        excludedFingerprints: Set<String> = emptySet(),
    ): List<LearningQuestion> {
        val requested = count.coerceIn(1, 25)
        val random = Random(seed xor concept.id.hashCode().toLong())
        val source = BuiltInCurriculum.tableNumberFor(concept.id)?.let { tableQuestions(it, random) }
            ?: when (concept.id) {
                BuiltInCurriculum.COUNT_1_20 -> countingQuestions(1, 20, random)
                BuiltInCurriculum.COUNT_21_50 -> countingQuestions(21, 50, random)
                BuiltInCurriculum.ADD_UNDER_10 -> additionQuestions(0..6, 1..4, carry = false, random = random)
                BuiltInCurriculum.ADD_UNDER_20 -> additionQuestions(3..13, 2..9, carry = null, random = random, maxAnswer = 20)
                BuiltInCurriculum.SUBTRACT_UNDER_10 -> subtractionQuestions(4..10, 1..6, borrow = false, random = random)
                BuiltInCurriculum.ADD_2D_1D_NO_CARRY -> addition2d1d(random)
                BuiltInCurriculum.ADD_2D_2D_NO_CARRY -> addition2d2d(random, carry = false)
                BuiltInCurriculum.ADD_WITH_CARRY -> addition2d2d(random, carry = true)
                BuiltInCurriculum.SUB_2D_1D_NO_BORROW -> subtraction2d1d(random)
                BuiltInCurriculum.SUB_2D_2D_NO_BORROW -> subtraction2d2d(random, borrow = false)
                BuiltInCurriculum.SUB_WITH_BORROW -> subtraction2d2d(random, borrow = true)
                BuiltInCurriculum.MISSING_NUMBER -> missingNumberQuestions(random)
                BuiltInCurriculum.GREATER_SMALLER -> greaterSmallerQuestions(random)
                BuiltInCurriculum.WORD_PROBLEMS -> wordProblems(random)
                BuiltInCurriculum.MULTIPLICATION_MEANING -> multiplicationMeaning(random)
                BuiltInCurriculum.GUJ_WORD_RECOGNITION -> gujaratiWordRecognition()
                BuiltInCurriculum.GUJ_SPELLING -> gujaratiSpelling()
                BuiltInCurriculum.GUJ_MISSING_LETTER -> gujaratiMissingLetters()
                BuiltInCurriculum.GUJ_READ_ALOUD -> gujaratiReadAloud()
                BuiltInCurriculum.GUJ_SENTENCE_COMPLETION -> gujaratiSentenceCompletion()
                BuiltInCurriculum.GUJ_WORD_MEANING -> gujaratiMeaning()
                BuiltInCurriculum.GUJ_SINGULAR_PLURAL -> gujaratiSingularPlural()
                BuiltInCurriculum.ENG_WORD_RECOGNITION -> englishWordRecognition()
                BuiltInCurriculum.ENG_SPELLING -> englishSpelling()
                BuiltInCurriculum.ENG_MISSING_LETTER -> englishMissingLetters()
                BuiltInCurriculum.ENG_READ_ALOUD -> englishReadAloud()
                BuiltInCurriculum.ENG_SENTENCE_COMPLETION -> englishSentenceCompletion()
                else -> emptyList()
            }

        val tagged = source.map { it.copy(conceptId = concept.id) }.shuffled(random)
        return QuestionVarietyPolicy.select(tagged, requested, excludedFingerprints)
            .mapIndexed { index, question -> question.copy(id = "${question.id}-${seed.toString(16)}-$index") }
    }

    private fun countingQuestions(min: Int, max: Int, random: Random): List<LearningQuestion> = buildList {
        repeat(24) { index ->
            val number = random.nextInt(min.coerceAtLeast(2), max)
            val before = index % 3 == 0
            val step = if (index % 4 == 0) 2 else 1
            val answer = if (before) number - step else number + step
            add(numericOne(
                id = "count-$number-$before-$step",
                prompt = if (before) "${gu(number)} પહેલાં $step અંક ગણો. કયો અંક આવશે?" else "${gu(number)} પછી $step અંક ગણો. કયો અંક આવશે?",
                answer = answer,
                hint = if (before) "પાછળ તરફ ધીમે ગણો." else "આગળ તરફ ધીમે ગણો.",
            ))
        }
    }

    private fun additionQuestions(
        first: IntRange,
        second: IntRange,
        carry: Boolean?,
        random: Random,
        maxAnswer: Int = 99,
    ): List<LearningQuestion> = buildList {
        repeat(36) { index ->
            var a: Int
            var b: Int
            do {
                a = random.nextInt(first.first, first.last + 1)
                b = random.nextInt(second.first, second.last + 1)
            } while (a + b > maxAnswer || (carry != null && ((a % 10 + b % 10 >= 10) != carry)))
            add(arithmeticQuestion("add-$a-$b-$index", a, b, "+", a + b, index))
        }
    }

    private fun subtractionQuestions(
        first: IntRange,
        second: IntRange,
        borrow: Boolean?,
        random: Random,
    ): List<LearningQuestion> = buildList {
        repeat(30) { index ->
            var a: Int
            var b: Int
            do {
                a = random.nextInt(first.first, first.last + 1)
                b = random.nextInt(second.first, second.last + 1).coerceAtMost(a)
            } while (b >= a || (borrow != null && ((a % 10 < b % 10) != borrow)))
            add(arithmeticQuestion("sub-$a-$b-$index", a, b, "−", a - b, index))
        }
    }

    private fun addition2d1d(random: Random): List<LearningQuestion> = buildList {
        repeat(40) { index ->
            val b = random.nextInt(2, 10)
            val tens = random.nextInt(1, 9)
            val ones = random.nextInt(0, 10 - b)
            val a = tens * 10 + ones
            add(arithmeticQuestion("add2d1d-$a-$b-$index", a, b, "+", a + b, index))
        }
    }

    private fun addition2d2d(random: Random, carry: Boolean): List<LearningQuestion> = buildList {
        repeat(56) { index ->
            var a: Int
            var b: Int
            do {
                a = random.nextInt(12, 78)
                b = random.nextInt(11, 78)
            } while (a + b > 99 || ((a % 10 + b % 10 >= 10) != carry))
            add(arithmeticQuestion(
                id = "add2d2d-$carry-$a-$b-$index",
                top = a,
                bottom = b,
                operator = "+",
                answer = a + b,
                style = index,
                hint = if (carry) "એકમનો સરવાળો ૧૦ કે વધુ થાય તો ૧ દશક આગળ લઈ જાઓ." else "પહેલા એકમ, પછી દશક ઉમેરો.",
                regrouping = carry,
            ))
        }
    }

    private fun subtraction2d1d(random: Random): List<LearningQuestion> = buildList {
        repeat(40) { index ->
            val b = random.nextInt(2, 10)
            val tens = random.nextInt(2, 10)
            val ones = random.nextInt(b, 10)
            val a = tens * 10 + ones
            add(arithmeticQuestion("sub2d1d-$a-$b-$index", a, b, "−", a - b, index))
        }
    }

    private fun subtraction2d2d(random: Random, borrow: Boolean): List<LearningQuestion> = buildList {
        repeat(56) { index ->
            var a: Int
            var b: Int
            do {
                a = random.nextInt(31, 100)
                b = random.nextInt(11, a)
            } while (((a % 10 < b % 10) != borrow))
            add(arithmeticQuestion(
                id = "sub2d2d-$borrow-$a-$b-$index",
                top = a,
                bottom = b,
                operator = "−",
                answer = a - b,
                style = index,
                hint = if (borrow) "એકમ પૂરતા ન હોય તો એક દશકને ૧૦ એકમમાં બદલો." else "પહેલા એકમ, પછી દશક ઘટાડો.",
                regrouping = borrow,
            ))
        }
    }

    private fun arithmeticQuestion(
        id: String,
        top: Int,
        bottom: Int,
        operator: String,
        answer: Int,
        style: Int,
        hint: String = if (operator == "+") "એકમથી શરૂ કરો; પછી દશક ગણો." else "એકમથી શરૂ કરો; પછી દશક ઘટાડો.",
        regrouping: Boolean = false,
    ): LearningQuestion {
        val isAddition = operator == "+"
        val prompt = when (style % 4) {
            0 -> "${gu(top)} $operator ${gu(bottom)} કેટલા?"
            1 -> if (isAddition) "રફ કામમાં ${gu(top)} અને ${gu(bottom)} ગોઠવી સરવાળો કરો." else "રફ કામમાં ${gu(top)} માંથી ${gu(bottom)} ઘટાડો."
            2 -> if (isAddition) "એક ડબ્બામાં ${gu(top)} રંગીન પેન્સિલ છે અને બીજા ડબ્બામાં ${gu(bottom)} છે. કુલ કેટલી?" else "${gu(top)} સ્ટિકરમાંથી ${gu(bottom)} આપી દીધા. કેટલા રહ્યા?"
            else -> if (isAddition) "દશક અને એકમ ગોઠવો: ${gu(top)} + ${gu(bottom)} = ?" else "દશક અને એકમ ગોઠવો: ${gu(top)} − ${gu(bottom)} = ?"
        }
        return LearningQuestion(
            id = id,
            promptGujarati = prompt,
            expectedAnswer = answer,
            evaluationMode = EvaluationMode.NUMERIC,
            activityType = if (style % 4 == 2) ActivityType.WORD_PROBLEM.name else ActivityType.QUESTION.name,
            hintGujarati = hint,
            arithmeticWork = ArithmeticWork(top, bottom, operator, regrouping),
        )
    }

    private fun tableQuestions(table: Int, random: Random): List<LearningQuestion> = (1..10).shuffled(random).flatMap { multiplier ->
        listOf(
            LearningQuestion(
                id = "table-$table-$multiplier-direct",
                promptGujarati = "${gu(table)} × ${gu(multiplier)} કેટલા?",
                expectedAnswer = table * multiplier,
                evaluationMode = EvaluationMode.NUMERIC,
                activityType = ActivityType.TABLES.name,
                hintGujarati = "${gu(table)} ને ${gu(multiplier)} વાર સમાન જૂથ તરીકે વિચારો.",
            ),
            LearningQuestion(
                id = "table-$table-$multiplier-story",
                promptGujarati = "${gu(multiplier)} થાળીમાં દરેકમાં ${gu(table)} લાડુ છે. કુલ કેટલા લાડુ?",
                expectedAnswer = table * multiplier,
                evaluationMode = EvaluationMode.NUMERIC,
                activityType = ActivityType.WORD_PROBLEM.name,
                hintGujarati = "${gu(table)} ને ${gu(multiplier)} વાર ઉમેરો.",
            ),
        )
    }

    private fun missingNumberQuestions(random: Random): List<LearningQuestion> = buildList {
        repeat(30) { index ->
            val a = random.nextInt(8, 60)
            val missing = random.nextInt(2, 10)
            val total = a + missing
            add(numericOne("missing-$a-$missing-$index", "${gu(a)} + ? = ${gu(total)}. ખૂટતો અંક કયો?", missing, "${gu(total)} માંથી ${gu(a)} ઘટાડો."))
        }
    }

    private fun greaterSmallerQuestions(random: Random): List<LearningQuestion> = buildList {
        repeat(24) { index ->
            val a = random.nextInt(20, 99)
            var b = random.nextInt(20, 99)
            if (a == b) b = (b + 1).coerceAtMost(99)
            val askLarger = index % 2 == 0
            val answer = if (askLarger) maxOf(a, b) else minOf(a, b)
            add(choice("compare-$a-$b-$index", if (askLarger) "મોટી સંખ્યા પસંદ કરો." else "નાની સંખ્યા પસંદ કરો.", gu(answer), listOf(gu(a), gu(b)).shuffled(random), "દશકનો અંક પહેલાં જુઓ."))
        }
    }

    private fun wordProblems(random: Random): List<LearningQuestion> = buildList {
        repeat(36) { index ->
            val addition = index % 2 == 0
            if (addition) {
                var a: Int
                var b: Int
                do {
                    a = random.nextInt(15, 70)
                    b = random.nextInt(11, 35)
                } while (a + b > 99)
                add(arithmeticQuestion("word-add-$a-$b-$index", a, b, "+", a + b, 2, regrouping = a % 10 + b % 10 >= 10))
            } else {
                val a = random.nextInt(35, 100)
                val b = random.nextInt(11, a)
                add(arithmeticQuestion("word-sub-$a-$b-$index", a, b, "−", a - b, 2, regrouping = a % 10 < b % 10))
            }
        }
    }

    private fun multiplicationMeaning(random: Random): List<LearningQuestion> = buildList {
        repeat(24) { index ->
            val groups = random.nextInt(2, 6)
            val inEach = random.nextInt(2, 7)
            add(numericOne("mult-$groups-$inEach-$index", "${gu(groups)} જૂથ છે. દરેક જૂથમાં ${gu(inEach)} વસ્તુ છે. કુલ કેટલી?", groups * inEach, "${gu(inEach)} ને ${gu(groups)} વાર ઉમેરો.").copy(activityType = ActivityType.TABLES.name))
        }
    }

    private fun gujaratiWordRecognition() = listOf(
        choice("guj-rec-1", "‘પુસ્તક’ શબ્દ પસંદ કરો.", "પુસ્તક", listOf("પુસ્તક", "પુસતક", "પુતસ્ક"), "શબ્દ ધીમે વાંચો."),
        choice("guj-rec-2", "‘પાણી’ શબ્દ પસંદ કરો.", "પાણી", listOf("પાણી", "પાની", "પણી"), "‘ણી’ના અવાજ પર ધ્યાન આપો."),
        choice("guj-rec-3", "‘શાળા’ શબ્દ પસંદ કરો.", "શાળા", listOf("સાળા", "શાળા", "શાલા"), "શ થી શરૂ થતો શબ્દ શોધો."),
        choice("guj-rec-4", "‘ફૂલ’ શબ્દ પસંદ કરો.", "ફૂલ", listOf("ફુલ", "ફૂલ", "ફૂળ"), "લાંબા ‘ઊ’ પર ધ્યાન આપો."),
        choice("guj-rec-5", "‘બાળક’ શબ્દ પસંદ કરો.", "બાળક", listOf("બાલક", "બાળક", "બાળક્"), "‘ળ’ ધરાવતો શબ્દ શોધો."),
    ).map { it.copy(activityType = ActivityType.VOCABULARY.name) }

    private fun gujaratiSpelling() = listOf(
        spelling("guj-spell-1", "કમળ", "ક થી શરૂ થાય છે અને છેલ્લે ળ આવે છે."),
        spelling("guj-spell-2", "પુસ્તક", "પુ પછી સ્તક આવે છે."),
        spelling("guj-spell-3", "પાણી", "‘ણી’નો અવાજ યાદ કરો."),
        spelling("guj-spell-4", "શાળા", "શ થી શરૂ થાય છે."),
        spelling("guj-spell-5", "ફૂલ", "વચ્ચે લાંબો ઊ છે."),
        spelling("guj-spell-6", "બાળક", "વચ્ચે ળ આવે છે."),
    )

    private fun gujaratiMissingLetters() = listOf(
        choice("guj-miss-1", "ક_ળ માં ખૂટતો અક્ષર કયો?", "મ", listOf("મ", "ન", "વ"), "શબ્દ ‘કમળ’ વિચારો."),
        choice("guj-miss-2", "પા_ી માં ખૂટતો અક્ષર કયો?", "ણ", listOf("ન", "ણ", "મ"), "‘પાણી’ બોલીને સાંભળો."),
        choice("guj-miss-3", "શા_ા માં ખૂટતો અક્ષર કયો?", "ળ", listOf("લ", "ળ", "ર"), "શબ્દ ‘શાળા’ છે."),
        choice("guj-miss-4", "ફૂ_ માં ખૂટતો અક્ષર કયો?", "લ", listOf("લ", "ળ", "ર"), "‘ફૂલ’ પૂરો બોલો."),
        choice("guj-miss-5", "બાળ_ માં ખૂટતો અક્ષર કયો?", "ક", listOf("ક", "ખ", "ગ"), "છેલ્લો અવાજ સાંભળો."),
    ).map { it.copy(activityType = ActivityType.MISSING_LETTER.name) }

    private fun gujaratiReadAloud() = listOf(
        readAloud("guj-read-1", "આ મારું પુસ્તક છે."), readAloud("guj-read-2", "મને ફૂલ ગમે છે."),
        readAloud("guj-read-3", "રવિ શાળાએ જાય છે."), readAloud("guj-read-4", "આકાશ વાદળી છે."),
        readAloud("guj-read-5", "પાણી પીવું સારું છે."),
    )

    private fun gujaratiSentenceCompletion() = listOf(
        choice("guj-sent-1", "હું ___ વાંચું છું.", "પુસ્તક", listOf("પુસ્તક", "પાણી", "આકાશ"), "વાંચી શકાય તેવી વસ્તુ પસંદ કરો."),
        choice("guj-sent-2", "આકાશ ___ છે.", "વાદળી", listOf("વાદળી", "મીઠું", "ગોળ"), "આકાશના રંગ વિશે વિચારો."),
        choice("guj-sent-3", "અમે ___ પીએ છીએ.", "પાણી", listOf("પાણી", "પથ્થર", "પેન્સિલ"), "પી શકાય તેવી વસ્તુ શોધો."),
        choice("guj-sent-4", "બાળક ___ જાય છે.", "શાળાએ", listOf("શાળાએ", "પુસ્તક", "વાદળી"), "ક્યાં જાય છે તે શબ્દ જોઈએ."),
        choice("guj-sent-5", "બગીચામાં ___ ખીલે છે.", "ફૂલ", listOf("ફૂલ", "પુસ્તક", "ખુરશી"), "બગીચામાં શું ખીલે?"),
    ).map { it.copy(activityType = ActivityType.VOCABULARY.name) }

    private fun gujaratiMeaning() = listOf(
        choice("guj-meaning-1", "‘મોટું’નો વિરુદ્ધ અર્થ કયો?", "નાનું", listOf("નાનું", "ઊંચું", "ઝડપી"), "મોટું નહીં, તો શું?"),
        choice("guj-meaning-2", "‘ખુશ’નો નજીકનો અર્થ કયો?", "આનંદિત", listOf("આનંદિત", "દુઃખી", "ભૂખ્યો"), "ખુશ હોય ત્યારે મન કેવું હોય?"),
        choice("guj-meaning-3", "‘ઝડપી’નો વિરુદ્ધ અર્થ કયો?", "ધીમું", listOf("ધીમું", "મોટું", "ગરમ"), "ઝડપ ઓછી હોય તો?"),
        choice("guj-meaning-4", "‘ઠંડું’નો વિરુદ્ધ અર્થ કયો?", "ગરમ", listOf("ગરમ", "નાનું", "ભીનું"), "હવામાનનો વિરુદ્ધ શબ્દ વિચારો."),
        choice("guj-meaning-5", "‘ઉપર’નો વિરુદ્ધ અર્થ કયો?", "નીચે", listOf("નીચે", "આગળ", "પાસે"), "દિશાનો વિરુદ્ધ શબ્દ શોધો."),
    ).map { it.copy(activityType = ActivityType.VOCABULARY.name) }

    private fun gujaratiSingularPlural() = listOf(
        choice("guj-pl-1", "‘બાળક’નું બહુવચન કયું?", "બાળકો", listOf("બાળકો", "બાળક", "બાળકી"), "એકથી વધુ બાળક હોય ત્યારે?"),
        choice("guj-pl-2", "‘પુસ્તક’નું બહુવચન કયું?", "પુસ્તકો", listOf("પુસ્તકો", "પુસ્તક", "પુસ્તકી"), "ઘણા પુસ્તક માટે અંત જુઓ."),
        choice("guj-pl-3", "‘છોકરો’નું બહુવચન કયું?", "છોકરાઓ", listOf("છોકરાઓ", "છોકરો", "છોકરી"), "ઘણા છોકરા માટે યોગ્ય રૂપ શોધો."),
        choice("guj-pl-4", "‘ઘોડો’નું બહુવચન કયું?", "ઘોડાઓ", listOf("ઘોડાઓ", "ઘોડો", "ઘોડી"), "એકથી વધુ ઘોડા વિચારો."),
        choice("guj-pl-5", "‘વસ્તુઓ’નું એકવચન કયું?", "વસ્તુ", listOf("વસ્તુ", "વસ્તુઓ", "વસ્તુંઓ"), "માત્ર એક હોય ત્યારે?"),
    ).map { it.copy(activityType = ActivityType.VOCABULARY.name) }

    private fun englishWordRecognition() = listOf(
        choice("eng-rec-1", "‘book’ શબ્દ પસંદ કરો.", "book", listOf("book", "bok", "boook"), "B-O-O-K."),
        choice("eng-rec-2", "‘school’ શબ્દ પસંદ કરો.", "school", listOf("school", "scool", "shool"), "S-C-H થી શરૂ થાય છે."),
        choice("eng-rec-3", "‘water’ શબ્દ પસંદ કરો.", "water", listOf("watre", "water", "woter"), "W-A-T-E-R."),
        choice("eng-rec-4", "‘mango’ શબ્દ પસંદ કરો.", "mango", listOf("mengo", "mango", "manggo"), "M-A-N-G-O."),
        choice("eng-rec-5", "‘tree’ શબ્દ પસંદ કરો.", "tree", listOf("tre", "tree", "tere"), "વચ્ચે double e છે."),
    ).map { it.copy(activityType = ActivityType.VOCABULARY.name) }

    private fun englishSpelling() = listOf(
        spelling("eng-spell-1", "book", "B થી શરૂ થાય છે; double o છે.", "Spell the word book", "en-IN"),
        spelling("eng-spell-2", "school", "S-C-H થી શરૂ થાય છે.", "Spell the word school", "en-IN"),
        spelling("eng-spell-3", "water", "W-A થી શરૂ થાય છે.", "Spell the word water", "en-IN"),
        spelling("eng-spell-4", "mango", "M થી શરૂ થાય છે અને O પર પૂરો થાય છે.", "Spell the word mango", "en-IN"),
        spelling("eng-spell-5", "tree", "T-R પછી double e આવે છે.", "Spell the word tree", "en-IN"),
        spelling("eng-spell-6", "ball", "B થી શરૂ થાય છે; double l છે.", "Spell the word ball", "en-IN"),
    )

    private fun englishMissingLetters() = listOf(
        choice("eng-miss-1", "b__k માં ખૂટતા letters કયા?", "oo", listOf("oo", "oa", "ou"), "શબ્દ book છે."),
        choice("eng-miss-2", "sch__l માં ખૂટતા letters કયા?", "oo", listOf("oo", "oa", "ee"), "school બોલીને સાંભળો."),
        choice("eng-miss-3", "wat_r માં ખૂટતો letter કયો?", "e", listOf("a", "e", "o"), "water = w-a-t-e-r."),
        choice("eng-miss-4", "m_ng_ માં બે ખૂટતા letters કયા?", "ao", listOf("ao", "eo", "aa"), "mango = m-a-n-g-o."),
        choice("eng-miss-5", "tr__ માં ખૂટતા letters કયા?", "ee", listOf("ee", "ea", "ie"), "tree માં double e છે."),
    ).map { it.copy(activityType = ActivityType.MISSING_LETTER.name) }

    private fun englishReadAloud() = listOf(
        readAloud("eng-read-1", "This is a book.", "en-IN"), readAloud("eng-read-2", "I like mango.", "en-IN"),
        readAloud("eng-read-3", "The ball is red.", "en-IN"), readAloud("eng-read-4", "This is my school.", "en-IN"),
        readAloud("eng-read-5", "The tree is green.", "en-IN"),
    )

    private fun englishSentenceCompletion() = listOf(
        choice("eng-sent-1", "This is a ___.", "book", listOf("book", "drink", "blue"), "વસ્તુનું નામ જોઈએ."),
        choice("eng-sent-2", "I drink ___.", "water", listOf("water", "tree", "ball"), "પી શકાય તેવી વસ્તુ પસંદ કરો."),
        choice("eng-sent-3", "The ___ is red.", "ball", listOf("ball", "drink", "read"), "એક વસ્તુનું નામ જોઈએ."),
        choice("eng-sent-4", "I go to ___.", "school", listOf("school", "green", "water"), "જગ્યાનું નામ જોઈએ."),
        choice("eng-sent-5", "The tree is ___.", "green", listOf("green", "book", "drink"), "રંગ પસંદ કરો."),
    ).map { it.copy(activityType = ActivityType.VOCABULARY.name) }

    private fun choice(id: String, prompt: String, answer: String, options: List<String>, hint: String) = LearningQuestion(
        id = id, promptGujarati = prompt, expectedText = answer, acceptedAnswers = listOf(answer),
        optionsGujarati = options, evaluationMode = EvaluationMode.MULTIPLE_CHOICE,
        activityType = ActivityType.MULTIPLE_CHOICE.name, hintGujarati = hint,
    )

    private fun spelling(id: String, word: String, hint: String, spokenPrefix: String? = null, languageTag: String = "gu-IN") = LearningQuestion(
        id = id,
        promptGujarati = "સાંભળો અને શબ્દ લખો. જવાબ સ્ક્રીન પર બતાવેલો નથી.",
        spokenPromptGujarati = spokenPrefix ?: "શબ્દ લખો: $word",
        speechLanguageTag = languageTag,
        recognitionLanguageTag = languageTag,
        expectedText = word,
        acceptedAnswers = listOf(word),
        evaluationMode = EvaluationMode.SHORT_TEXT,
        activityType = ActivityType.SPELLING.name,
        hintGujarati = hint,
    )

    private fun readAloud(id: String, text: String, recognitionLanguageTag: String = "gu-IN") = LearningQuestion(
        id = id,
        promptGujarati = "વાંચીને બોલો: $text",
        spokenPromptGujarati = "આ લખાણ વાંચીને બોલો.",
        speechLanguageTag = "gu-IN",
        recognitionLanguageTag = recognitionLanguageTag,
        expectedText = text,
        acceptedAnswers = listOf(text),
        evaluationMode = EvaluationMode.SHORT_TEXT,
        activityType = ActivityType.READING.name,
        hintGujarati = "એક-એક શબ્દ ધીમે વાંચો.",
    )

    private fun numericOne(id: String, prompt: String, answer: Int, hint: String) = LearningQuestion(
        id = id, promptGujarati = prompt, expectedAnswer = answer,
        evaluationMode = EvaluationMode.NUMERIC, activityType = ActivityType.QUESTION.name, hintGujarati = hint,
    )

    private fun gu(number: Int): String = number.toString().map { digit ->
        when (digit) {
            '0' -> '૦'; '1' -> '૧'; '2' -> '૨'; '3' -> '૩'; '4' -> '૪'
            '5' -> '૫'; '6' -> '૬'; '7' -> '૭'; '8' -> '૮'; '9' -> '૯'
            else -> digit
        }
    }.joinToString("")
}
