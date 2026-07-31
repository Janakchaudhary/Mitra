package com.mitra.learning.learning.curriculum

import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion

/** Local, deterministic Standard 2 drills. No network or LLM is required. */
object Standard2SkillActivityFactory {
    fun create(concept: ConceptEntity, count: Int): List<LearningQuestion> {
        val requested = count.coerceIn(1, 8)
        val source = BuiltInCurriculum.tableNumberFor(concept.id)?.let(::tableQuestions)
            ?: when (concept.id) {
                BuiltInCurriculum.COUNT_1_20 -> numeric("count", listOf(
                    "૪ પછી કયો અંક આવે?" to 5, "૯ પછી કયો અંક આવે?" to 10,
                    "૧૪ પછી કયો અંક આવે?" to 15, "૮ પહેલાં કયો અંક આવે?" to 7,
                    "૧૯ પછી કયો અંક આવે?" to 20,
                ))
                BuiltInCurriculum.COUNT_21_50 -> numeric("count50", listOf(
                    "૨૯ પછી કયો અંક આવે?" to 30, "૩૪ પહેલાં કયો અંક આવે?" to 33,
                    "૪૧ પછી કયો અંક આવે?" to 42, "૨૫ પછી બે અંક ગણો. કયો અંક આવશે?" to 27,
                    "૪૯ પછી કયો અંક આવે?" to 50,
                ))
                BuiltInCurriculum.ADD_UNDER_10 -> numeric("add10", listOf(
                    "૩ + ૨ કેટલા?" to 5, "૪ + ૩ કેટલા?" to 7, "૨ + ૬ કેટલા?" to 8,
                    "૫ + ૪ કેટલા?" to 9, "૧ + ૬ કેટલા?" to 7,
                ))
                BuiltInCurriculum.ADD_UNDER_20 -> numeric("add20", listOf(
                    "૮ + ૫ કેટલા?" to 13, "૯ + ૭ કેટલા?" to 16, "૧૧ + ૪ કેટલા?" to 15,
                    "૬ + ૧૨ કેટલા?" to 18, "૧૦ + ૯ કેટલા?" to 19,
                ))
                BuiltInCurriculum.SUBTRACT_UNDER_10 -> numeric("sub10", listOf(
                    "૭ - ૨ કેટલા?" to 5, "૯ - ૩ કેટલા?" to 6, "૮ - ૫ કેટલા?" to 3,
                    "૬ - ૧ કેટલા?" to 5, "૧૦ - ૪ કેટલા?" to 6,
                ))
                BuiltInCurriculum.ADD_2D_1D_NO_CARRY -> numeric("add2d1d", listOf(
                    "૨૩ + ૪ કેટલા?" to 27, "૪૧ + ૭ કેટલા?" to 48, "૩૨ + ૫ કેટલા?" to 37,
                    "૬૧ + ૮ કેટલા?" to 69, "૫૪ + ૩ કેટલા?" to 57,
                ), "એકમના અંકથી શરૂ કરો; અહીં કેરિ કરવાની જરૂર નથી.")
                BuiltInCurriculum.ADD_2D_2D_NO_CARRY -> numeric("add2d2d", listOf(
                    "૨૧ + ૧૬ કેટલા?" to 37, "૩૨ + ૨૫ કેટલા?" to 57, "૪૩ + ૧૪ કેટલા?" to 57,
                    "૫૧ + ૨૮ કેટલા?" to 79, "૬૨ + ૧૭ કેટલા?" to 79,
                ), "પહેલા એકમ ઉમેરો, પછી દશક ઉમેરો.")
                BuiltInCurriculum.ADD_WITH_CARRY -> numeric("carry", listOf(
                    "૨૮ + ૭ કેટલા?" to 35, "૨૭ + ૧૮ કેટલા?" to 45, "૩૬ + ૨૭ કેટલા?" to 63,
                    "૪૮ + ૧૫ કેટલા?" to 63, "૫૭ + ૨૬ કેટલા?" to 83,
                ), "એકમનો સરવાળો ૧૦ કે વધુ થાય તો ૧ દશક આગળ લઈ જાઓ.")
                BuiltInCurriculum.SUB_2D_1D_NO_BORROW -> numeric("sub2d1d", listOf(
                    "૩૪ - ૨ કેટલા?" to 32, "૪૮ - ૫ કેટલા?" to 43, "૬૭ - ૬ કેટલા?" to 61,
                    "૫૯ - ૭ કેટલા?" to 52, "૨૬ - ૪ કેટલા?" to 22,
                ), "એકમમાંથી જ ઘટાડો; ઉધાર લેવાની જરૂર નથી.")
                BuiltInCurriculum.SUB_2D_2D_NO_BORROW -> numeric("sub2d2d", listOf(
                    "૪૬ - ૨૩ કેટલા?" to 23, "૭૮ - ૩૫ કેટલા?" to 43, "૬૯ - ૨૭ કેટલા?" to 42,
                    "૫૭ - ૧૪ કેટલા?" to 43, "૮૮ - ૪૬ કેટલા?" to 42,
                ), "પહેલા એકમ, પછી દશક ઘટાડો.")
                BuiltInCurriculum.SUB_WITH_BORROW -> numeric("borrow", listOf(
                    "૩૨ - ૭ કેટલા?" to 25, "૫૨ - ૨૮ કેટલા?" to 24, "૬૧ - ૩૬ કેટલા?" to 25,
                    "૭૩ - ૪૮ કેટલા?" to 25, "૮૨ - ૫૭ કેટલા?" to 25,
                ), "એકમ પૂરતા ન હોય તો એક દશકને ૧૦ એકમમાં બદલો.")
                BuiltInCurriculum.MISSING_NUMBER -> numeric("missing", listOf(
                    "૧૨ + ? = ૧૯. ખૂટતો અંક કયો?" to 7, "? + ૬ = ૧૫. ખૂટતો અંક કયો?" to 9,
                    "૧૮ - ? = ૧૧. ખૂટતો અંક કયો?" to 7, "? - ૫ = ૧૩. ખૂટતો અંક કયો?" to 18,
                    "૨૪ + ? = ૩૦. ખૂટતો અંક કયો?" to 6,
                ), "જાણીતો જવાબ મેળવવા પાછળથી ગણો.")
                BuiltInCurriculum.GREATER_SMALLER -> greaterSmallerQuestions()
                BuiltInCurriculum.WORD_PROBLEMS -> wordProblems()
                BuiltInCurriculum.MULTIPLICATION_MEANING -> multiplicationMeaning()
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

        if (source.isEmpty()) return emptyList()
        return List(requested) { index ->
            val base = source[index % source.size]
            base.copy(id = "${base.id}-$index")
        }
    }

    private fun numeric(prefix: String, items: List<Pair<String, Int>>, hint: String = "ધીમે ધીમે એક-એક પગલું કરો.") =
        items.mapIndexed { index, (prompt, answer) ->
            LearningQuestion(
                id = "$prefix-$index",
                promptGujarati = prompt,
                expectedAnswer = answer,
                evaluationMode = EvaluationMode.NUMERIC,
                activityType = ActivityType.QUESTION.name,
                hintGujarati = hint,
            )
        }

    private fun tableQuestions(table: Int): List<LearningQuestion> = (1..10).map { multiplier ->
        LearningQuestion(
            id = "table-$table-$multiplier",
            promptGujarati = "$table × $multiplier કેટલા?",
            expectedAnswer = table * multiplier,
            evaluationMode = EvaluationMode.NUMERIC,
            activityType = ActivityType.TABLES.name,
            hintGujarati = "$table ને $multiplier વાર સમાન જૂથ તરીકે વિચારો.",
        )
    }

    private fun greaterSmallerQuestions() = listOf(
        choice("greater-1", "મોટી સંખ્યા પસંદ કરો.", "૪૮", listOf("૪૮", "૩૮"), "દશકનો અંક પહેલાં જુઓ."),
        choice("greater-2", "નાની સંખ્યા પસંદ કરો.", "૨૬", listOf("૬૨", "૨૬"), "દશકની સરખામણી કરો."),
        choice("greater-3", "કઈ સંખ્યા મોટી છે?", "૭૧", listOf("૬૯", "૭૧", "૬૧"), "૭ દશક, ૬ દશક કરતાં મોટું છે."),
        choice("greater-4", "કઈ સંખ્યા નાની છે?", "૩૯", listOf("૪૦", "૩૯", "૪૯"), "૩ દશકવાળી સંખ્યા શોધો."),
        choice("greater-5", "૫૫ અને ૫૨ માં મોટી કઈ?", "૫૫", listOf("૫૨", "૫૫"), "દશક સમાન છે, હવે એકમ જુઓ."),
    )

    private fun wordProblems() = numeric("word", listOf(
        "રિયા પાસે ૨૩ પેન્સિલ છે. તેને ૪ વધુ મળે. કુલ કેટલી?" to 27,
        "એક ડબ્બામાં ૩૨ બોલ હતા. ૫ કાઢ્યા. કેટલા રહ્યા?" to 27,
        "બગીચામાં ૨૧ લાલ અને ૧૬ પીળા ફૂલ છે. કુલ કેટલા?" to 37,
        "દુકાનમાં ૪૬ પતંગ હતા. ૨૩ વેચાયા. કેટલા રહ્યા?" to 23,
        "મીરા પાસે ૨૮ સ્ટિકર હતા. તેને ૭ વધુ મળ્યા. હવે કેટલા?" to 35,
    ), "વાર્તામાં વસ્તુઓ વધે છે કે ઘટે છે તે પહેલાં નક્કી કરો.").map { it.copy(activityType = ActivityType.WORD_PROBLEM.name) }

    private fun multiplicationMeaning() = listOf(
        numericOne("mult-1", "૩ જૂથ છે. દરેક જૂથમાં ૪ કેરી છે. કુલ કેટલી?", 12, "૪ + ૪ + ૪ કરો."),
        numericOne("mult-2", "૨ + ૨ + ૨ + ૨ કેટલા?", 8, "આ ૪ જૂથ × ૨ જેવું છે."),
        choice("mult-3", "૩ × ૪ નો વારંવાર સરવાળો કયો?", "૪ + ૪ + ૪", listOf("૩ + ૩ + ૩ + ૩", "૪ + ૪ + ૪", "૩ + ૪"), "૩ સમાન જૂથ, દરેકમાં ૪."),
        numericOne("mult-4", "૫ થાળીમાં દરેકમાં ૨ લાડુ છે. કુલ કેટલા?", 10, "૨ ને ૫ વાર ઉમેરો."),
        choice("mult-5", "૪ + ૪ + ૪ ને ગુણાકારમાં કેવી રીતે લખી શકાય?", "૩ × ૪", listOf("૩ × ૪", "૪ × ૪", "૩ × ૩"), "કેટલા સમાન જૂથ છે તે પહેલાં જુઓ."),
    ).map { it.copy(activityType = ActivityType.TABLES.name) }

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
        readAloud("guj-read-1", "આ મારું પુસ્તક છે."),
        readAloud("guj-read-2", "મને ફૂલ ગમે છે."),
        readAloud("guj-read-3", "રવિ શાળાએ જાય છે."),
        readAloud("guj-read-4", "આકાશ વાદળી છે."),
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
        choice("guj-meaning-4", "‘ઠંડું’નો વિરુદ્ધ અર્થ કયો?", "ગરમ", listOf("ગરમ", "નાનું", "ભીનું"), "હવામાનના વિરુદ્ધ શબ્દ વિશે વિચારો."),
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
        spelling("eng-spell-1", "book", "B થી શરૂ થાય છે; double o છે.", spokenPrefix = "Spell the word book", languageTag = "en-IN"),
        spelling("eng-spell-2", "school", "S-C-H થી શરૂ થાય છે.", spokenPrefix = "Spell the word school", languageTag = "en-IN"),
        spelling("eng-spell-3", "water", "W-A થી શરૂ થાય છે.", spokenPrefix = "Spell the word water", languageTag = "en-IN"),
        spelling("eng-spell-4", "mango", "M થી શરૂ થાય છે અને O પર પૂરો થાય છે.", spokenPrefix = "Spell the word mango", languageTag = "en-IN"),
        spelling("eng-spell-5", "tree", "T-R પછી double e આવે છે.", spokenPrefix = "Spell the word tree", languageTag = "en-IN"),
        spelling("eng-spell-6", "ball", "B થી શરૂ થાય છે; double l છે.", spokenPrefix = "Spell the word ball", languageTag = "en-IN"),
    )

    private fun englishMissingLetters() = listOf(
        choice("eng-miss-1", "b__k માં ખૂટતા letters કયા?", "oo", listOf("oo", "oa", "ou"), "શબ્દ book છે."),
        choice("eng-miss-2", "sch__l માં ખૂટતા letters કયા?", "oo", listOf("oo", "oa", "ee"), "school બોલીને સાંભળો."),
        choice("eng-miss-3", "wat_r માં ખૂટતો letter કયો?", "e", listOf("a", "e", "o"), "water = w-a-t-e-r."),
        choice("eng-miss-4", "m_ng_ માં બે ખૂટતા letters કયા?", "ao", listOf("ao", "eo", "aa"), "mango = m-a-n-g-o."),
        choice("eng-miss-5", "tr__ માં ખૂટતા letters કયા?", "ee", listOf("ee", "ea", "ie"), "tree માં double e છે."),
    ).map { it.copy(activityType = ActivityType.MISSING_LETTER.name) }

    private fun englishReadAloud() = listOf(
        readAloud("eng-read-1", "This is a book.", recognitionLanguageTag = "en-IN"),
        readAloud("eng-read-2", "I like mango.", recognitionLanguageTag = "en-IN"),
        readAloud("eng-read-3", "The ball is red.", recognitionLanguageTag = "en-IN"),
        readAloud("eng-read-4", "This is my school.", recognitionLanguageTag = "en-IN"),
        readAloud("eng-read-5", "The tree is green.", recognitionLanguageTag = "en-IN"),
    )

    private fun englishSentenceCompletion() = listOf(
        choice("eng-sent-1", "This is a ___.", "book", listOf("book", "drink", "blue"), "વસ્તુનું નામ જોઈએ."),
        choice("eng-sent-2", "I drink ___.", "water", listOf("water", "tree", "ball"), "પી શકાય તેવી વસ્તુ પસંદ કરો."),
        choice("eng-sent-3", "The ___ is red.", "ball", listOf("ball", "drink", "read"), "એક વસ્તુનું નામ જોઈએ."),
        choice("eng-sent-4", "I go to ___.", "school", listOf("school", "green", "water"), "જગ્યાનું નામ જોઈએ."),
        choice("eng-sent-5", "The tree is ___.", "green", listOf("green", "book", "drink"), "રંગ પસંદ કરો."),
    ).map { it.copy(activityType = ActivityType.VOCABULARY.name) }

    private fun choice(id: String, prompt: String, answer: String, options: List<String>, hint: String) = LearningQuestion(
        id = id,
        promptGujarati = prompt,
        expectedText = answer,
        acceptedAnswers = listOf(answer),
        optionsGujarati = options,
        evaluationMode = EvaluationMode.MULTIPLE_CHOICE,
        activityType = ActivityType.MULTIPLE_CHOICE.name,
        hintGujarati = hint,
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
        id = id,
        promptGujarati = prompt,
        expectedAnswer = answer,
        evaluationMode = EvaluationMode.NUMERIC,
        activityType = ActivityType.QUESTION.name,
        hintGujarati = hint,
    )
}
