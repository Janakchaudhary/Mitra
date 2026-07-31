package com.mitra.learning.learning.curriculum

import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity

/**
 * Offline Standard 2 skill catalogue.
 *
 * Every item is intentionally a separate ConceptEntity so Room keeps separate mastery for
 * carrying, borrowing, each multiplication table, Gujarati spelling, English spelling, etc.
 */
object BuiltInCurriculum {
    // Core number sense / early arithmetic.
    const val COUNT_1_20 = "builtin-math-count-1-20"
    const val COUNT_21_50 = "builtin-math-count-21-50"
    const val ADD_UNDER_10 = "builtin-math-add-under-10"
    const val ADD_UNDER_20 = "builtin-math-add-under-20"
    const val SUBTRACT_UNDER_10 = "builtin-math-subtract-under-10"

    // Standard 2 arithmetic progression.
    const val ADD_2D_1D_NO_CARRY = "builtin-math-add-2d-1d-no-carry"
    const val ADD_2D_2D_NO_CARRY = "builtin-math-add-2d-2d-no-carry"
    const val ADD_WITH_CARRY = "builtin-math-add-with-carry"
    const val SUB_2D_1D_NO_BORROW = "builtin-math-sub-2d-1d-no-borrow"
    const val SUB_2D_2D_NO_BORROW = "builtin-math-sub-2d-2d-no-borrow"
    const val SUB_WITH_BORROW = "builtin-math-sub-with-borrow"
    const val MISSING_NUMBER = "builtin-math-missing-number"
    const val GREATER_SMALLER = "builtin-math-greater-smaller"
    const val WORD_PROBLEMS = "builtin-math-word-problems"

    // Multiplication meaning and individual table mastery.
    const val MULTIPLICATION_MEANING = "builtin-math-multiplication-meaning"
    const val TABLE_2 = "builtin-math-table-2"
    const val TABLE_3 = "builtin-math-table-3"
    const val TABLE_4 = "builtin-math-table-4"
    const val TABLE_5 = "builtin-math-table-5"
    const val TABLE_6 = "builtin-math-table-6"
    const val TABLE_7 = "builtin-math-table-7"
    const val TABLE_8 = "builtin-math-table-8"
    const val TABLE_9 = "builtin-math-table-9"
    const val TABLE_10 = "builtin-math-table-10"

    // Gujarati language skills.
    const val GUJ_WORD_RECOGNITION = "builtin-guj-word-recognition"
    const val GUJ_SPELLING = "builtin-guj-spelling"
    const val GUJ_MISSING_LETTER = "builtin-guj-missing-letter"
    const val GUJ_READ_ALOUD = "builtin-guj-read-aloud"
    const val GUJ_SENTENCE_COMPLETION = "builtin-guj-sentence-completion"
    const val GUJ_WORD_MEANING = "builtin-guj-word-meaning"
    const val GUJ_SINGULAR_PLURAL = "builtin-guj-singular-plural"

    // English language skills.
    const val ENG_WORD_RECOGNITION = "builtin-eng-word-recognition"
    const val ENG_SPELLING = "builtin-eng-spelling"
    const val ENG_MISSING_LETTER = "builtin-eng-missing-letter"
    const val ENG_READ_ALOUD = "builtin-eng-read-aloud"
    const val ENG_SENTENCE_COMPLETION = "builtin-eng-sentence-completion"

    val concepts: List<ConceptEntity> = listOf(
        concept(COUNT_1_20, 10, "Mathematics", "૧ થી ૨૦ સુધી ગણતરી", "Counting 1 to 20", "૧ થી ૨૦ સુધીના અંકો ઓળખવા અને ક્રમમાં કહેવા.", "બાળક ૧ થી ૨૦ સુધીના અંકો ઓળખી અને ક્રમમાં કહી શકે.", 1),
        concept(ADD_UNDER_10, 20, "Mathematics", "૧૦ સુધી સરવાળો", "Addition under 10", "બે નાના અંકોનો સરવાળો કરીને ૧૦ સુધીનો જવાબ શોધવો.", "બાળક ૧૦ સુધીના સરળ સરવાળા ઉકેલી શકે.", 1),
        concept(SUBTRACT_UNDER_10, 30, "Mathematics", "૧૦ સુધી બાદબાકી", "Subtraction under 10", "૧૦ સુધીના અંકોમાં સરળ બાદબાકી સમજવી.", "બાળક ૧૦ સુધીની સરળ બાદબાકી ઉકેલી શકે.", 1),
        concept(COUNT_21_50, 40, "Mathematics", "૨૧ થી ૫૦ સુધી ગણતરી", "Counting 21 to 50", "૨૧ થી ૫૦ સુધીના અંકો ઓળખવા અને પહેલાં/પછીનો અંક શોધવો.", "બાળક ૨૧ થી ૫૦ સુધીના અંકોનો ક્રમ સમજી શકે.", 2),
        concept(ADD_UNDER_20, 50, "Mathematics", "૨૦ સુધી સરવાળો", "Addition under 20", "બે અંકોનો સરવાળો કરીને ૨૦ સુધીનો જવાબ શોધવો.", "બાળક ૨૦ સુધીના સરળ સરવાળા ઉકેલી શકે.", 2),

        concept(ADD_2D_1D_NO_CARRY, 60, "Mathematics", "બે અંક + એક અંક", "2-digit + 1-digit", "કેરિ વગર બે અંકની સંખ્યામાં એક અંક ઉમેરવો.", "બાળક ૨૩ + ૪ જેવા સરવાળા ઉકેલી શકે.", 2),
        concept(ADD_2D_2D_NO_CARRY, 70, "Mathematics", "બે અંકનો સરવાળો — કેરિ વગર", "2-digit addition without carrying", "એકમ અને દશક અલગ ગણી બે બે-અંક સંખ્યાનો સરવાળો કરવો.", "બાળક ૨૧ + ૧૬ જેવા સરવાળા ઉકેલી શકે.", 2),
        concept(ADD_WITH_CARRY, 80, "Mathematics", "બે અંકનો સરવાળો — કેરિ સાથે", "2-digit addition with carrying", "એકમનો સરવાળો ૧૦ કે વધુ થાય ત્યારે દશકમાં કેરિ કરવી.", "બાળક ૨૭ + ૧૮ જેવા કેરિવાળા સરવાળા ઉકેલી શકે.", 3),
        concept(SUB_2D_1D_NO_BORROW, 90, "Mathematics", "બે અંક - એક અંક", "2-digit - 1-digit", "ઉધાર લીધા વગર બે અંકની સંખ્યામાંથી એક અંક ઘટાડવો.", "બાળક ૩૪ - ૨ જેવા બાદબાકી ઉકેલી શકે.", 2),
        concept(SUB_2D_2D_NO_BORROW, 100, "Mathematics", "બે અંકની બાદબાકી — ઉધાર વગર", "2-digit subtraction without borrowing", "એકમ અને દશક અલગ ગણી ઉધાર વગર બે-અંક બાદબાકી કરવી.", "બાળક ૪૬ - ૨૩ જેવા ઉદાહરણ ઉકેલી શકે.", 2),
        concept(SUB_WITH_BORROW, 110, "Mathematics", "બે અંકની બાદબાકી — ઉધાર સાથે", "2-digit subtraction with borrowing", "એકમ પૂરતા ન હોય ત્યારે દશકમાંથી ઉધાર લેવું.", "બાળક ૫૨ - ૨૮ જેવી બાદબાકી ઉકેલી શકે.", 3),
        concept(MISSING_NUMBER, 120, "Mathematics", "ખૂટતો અંક શોધો", "Missing number", "સરવાળા અથવા બાદબાકીમાંથી ખૂટતી સંખ્યા શોધવી.", "બાળક ૧૨ + ? = ૧૯ જેવા પ્રશ્નમાં ખૂટતો અંક શોધી શકે.", 2),
        concept(GREATER_SMALLER, 130, "Mathematics", "મોટી-નાની સંખ્યા", "Greater and smaller numbers", "બે સંખ્યાની સરખામણી કરીને મોટી, નાની અથવા સમાન ઓળખવી.", "બાળક બે બે-અંક સંખ્યાની સાચી સરખામણી કરી શકે.", 2),
        concept(WORD_PROBLEMS, 140, "Mathematics", "સરવાળા-બાદબાકીની વાર્તા", "Addition and subtraction word problems", "દૈનિક જીવનની ટૂંકી વાર્તામાં કયો હિસાબ કરવો તે ઓળખવો.", "બાળક સરળ શબ્દપ્રશ્નને સરવાળા કે બાદબાકીથી ઉકેલી શકે.", 3),
        concept(MULTIPLICATION_MEANING, 150, "Mathematics", "ગુણાકાર એટલે સમાન જૂથો", "Meaning of multiplication", "સમાન જૂથો અને વારંવાર સરવાળાથી ગુણાકાર સમજવો.", "બાળક ૩ જૂથમાં ૪ વસ્તુ એટલે ૪ + ૪ + ૪ અને ૩ × ૪ સમજાવી શકે.", 2),
        tableConcept(TABLE_2, 160, 2),
        tableConcept(TABLE_3, 170, 3),
        tableConcept(TABLE_4, 180, 4),
        tableConcept(TABLE_5, 190, 5),
        tableConcept(TABLE_6, 200, 6),
        tableConcept(TABLE_7, 210, 7),
        tableConcept(TABLE_8, 220, 8),
        tableConcept(TABLE_9, 230, 9),
        tableConcept(TABLE_10, 240, 10),

        concept(GUJ_WORD_RECOGNITION, 300, "Gujarati", "ગુજરાતી શબ્દ ઓળખ", "Gujarati word recognition", "સામાન્ય ધોરણ ૨ના ગુજરાતી શબ્દો ઓળખવા.", "બાળક સામાન્ય ગુજરાતી શબ્દ સાચો ઓળખી શકે.", 1),
        concept(GUJ_SPELLING, 310, "Gujarati", "ગુજરાતી જોડણી", "Gujarati spelling", "સાંભળેલા સામાન્ય ગુજરાતી શબ્દની સાચી જોડણી લખવી.", "બાળક સામાન્ય ગુજરાતી શબ્દોની સાચી જોડણી કરી શકે.", 2),
        concept(GUJ_MISSING_LETTER, 320, "Gujarati", "ખૂટતો અક્ષર", "Missing Gujarati letter", "શબ્દમાં ખૂટતો અક્ષર ઓળખવો.", "બાળક સરળ શબ્દમાં ખૂટતો અક્ષર ભરી શકે.", 2),
        concept(GUJ_READ_ALOUD, 330, "Gujarati", "ગુજરાતી વાંચીને બોલવું", "Gujarati read aloud", "ટૂંકા ગુજરાતી શબ્દ અને વાક્ય વાંચીને બોલવા.", "બાળક સરળ ગુજરાતી વાક્ય સ્પષ્ટ વાંચી શકે.", 2),
        concept(GUJ_SENTENCE_COMPLETION, 340, "Gujarati", "વાક્ય પૂર્ણ કરો", "Gujarati sentence completion", "અર્થ પ્રમાણે યોગ્ય શબ્દથી સરળ વાક્ય પૂર્ણ કરવું.", "બાળક યોગ્ય શબ્દથી સરળ ગુજરાતી વાક્ય પૂર્ણ કરી શકે.", 2),
        concept(GUJ_WORD_MEANING, 350, "Gujarati", "શબ્દનો અર્થ", "Gujarati word meaning", "સરળ ગુજરાતી શબ્દનો અર્થ અથવા સંબંધિત વિચાર ઓળખવો.", "બાળક સામાન્ય શબ્દનો સરળ અર્થ ઓળખી શકે.", 2),
        concept(GUJ_SINGULAR_PLURAL, 360, "Gujarati", "એકવચન અને બહુવચન", "Gujarati singular and plural", "એક વસ્તુ અને ઘણી વસ્તુ દર્શાવતા શબ્દરૂપ ઓળખવા.", "બાળક સરળ એકવચન-બહુવચન જોડીઓ ઓળખી શકે.", 2),

        concept(ENG_WORD_RECOGNITION, 400, "English", "English word ઓળખ", "English word recognition", "Standard 2 માટેના સામાન્ય English words ઓળખવા.", "બાળક સામાન્ય English word ઓળખી શકે.", 1, language = "en-IN"),
        concept(ENG_SPELLING, 410, "English", "English spelling", "English spelling", "સાંભળેલા સરળ English word ની spelling લખવી.", "બાળક common Standard 2 English words spell કરી શકે.", 2, language = "en-IN"),
        concept(ENG_MISSING_LETTER, 420, "English", "English missing letter", "English missing letter", "English word માં ખૂટતો letter ઓળખવો.", "બાળક simple English word નો missing letter ભરી શકે.", 2, language = "en-IN"),
        concept(ENG_READ_ALOUD, 430, "English", "English વાંચીને બોલવું", "English read aloud", "ટૂંકા English word અને sentence વાંચીને બોલવા.", "બાળક simple English sentence વાંચી શકે.", 2, language = "en-IN"),
        concept(ENG_SENTENCE_COMPLETION, 440, "English", "English sentence completion", "English sentence completion", "યોગ્ય common word થી simple English sentence પૂર્ણ કરવું.", "બાળક simple English sentence યોગ્ય શબ્દથી પૂર્ણ કરી શકે.", 2, language = "en-IN"),
    )

    val prerequisites: List<ConceptPrerequisiteEntity> = buildList {
        add(prereq(COUNT_21_50, COUNT_1_20))
        add(prereq(ADD_UNDER_20, ADD_UNDER_10))
        add(prereq(ADD_2D_1D_NO_CARRY, ADD_UNDER_20))
        add(prereq(ADD_2D_2D_NO_CARRY, ADD_2D_1D_NO_CARRY))
        add(prereq(ADD_WITH_CARRY, ADD_2D_2D_NO_CARRY))
        add(prereq(SUB_2D_1D_NO_BORROW, SUBTRACT_UNDER_10))
        add(prereq(SUB_2D_2D_NO_BORROW, SUB_2D_1D_NO_BORROW))
        add(prereq(SUB_WITH_BORROW, SUB_2D_2D_NO_BORROW))
        add(prereq(GREATER_SMALLER, COUNT_21_50))
        add(prereq(MISSING_NUMBER, ADD_UNDER_20))
        add(prereq(WORD_PROBLEMS, ADD_2D_1D_NO_CARRY))
        add(prereq(WORD_PROBLEMS, SUB_2D_1D_NO_BORROW))
        add(prereq(MULTIPLICATION_MEANING, ADD_UNDER_20))
        listOf(TABLE_2, TABLE_3, TABLE_4, TABLE_5, TABLE_6, TABLE_7, TABLE_8, TABLE_9, TABLE_10)
            .forEach { add(prereq(it, MULTIPLICATION_MEANING)) }

        add(prereq(GUJ_SPELLING, GUJ_WORD_RECOGNITION))
        add(prereq(GUJ_MISSING_LETTER, GUJ_WORD_RECOGNITION))
        add(prereq(GUJ_READ_ALOUD, GUJ_WORD_RECOGNITION))
        add(prereq(GUJ_SENTENCE_COMPLETION, GUJ_WORD_RECOGNITION))
        add(prereq(GUJ_WORD_MEANING, GUJ_WORD_RECOGNITION))
        add(prereq(GUJ_SINGULAR_PLURAL, GUJ_WORD_RECOGNITION))

        add(prereq(ENG_SPELLING, ENG_WORD_RECOGNITION))
        add(prereq(ENG_MISSING_LETTER, ENG_WORD_RECOGNITION))
        add(prereq(ENG_READ_ALOUD, ENG_WORD_RECOGNITION))
        add(prereq(ENG_SENTENCE_COMPLETION, ENG_WORD_RECOGNITION))
    }

    fun tableNumberFor(conceptId: String): Int? = when (conceptId) {
        TABLE_2 -> 2
        TABLE_3 -> 3
        TABLE_4 -> 4
        TABLE_5 -> 5
        TABLE_6 -> 6
        TABLE_7 -> 7
        TABLE_8 -> 8
        TABLE_9 -> 9
        TABLE_10 -> 10
        else -> null
    }

    private fun tableConcept(id: String, order: Int, table: Int): ConceptEntity = concept(
        id = id,
        order = order,
        subject = "Mathematics",
        gujarati = "$table નો પહાડો",
        english = "Table of $table",
        description = "$table ના ગુણાકારના facts ૧ થી ૧૦ સુધી સમજવા અને યાદ કરવાના.",
        outcome = "બાળક $table × ૧ થી $table × ૧૦ સુધીના facts ઉકેલી શકે.",
        difficulty = if (table <= 5 || table == 10) 2 else 3,
    )

    private fun prereq(conceptId: String, prerequisiteId: String) =
        ConceptPrerequisiteEntity(conceptId, prerequisiteId)

    private fun concept(
        id: String,
        order: Int,
        subject: String,
        gujarati: String,
        english: String,
        description: String,
        outcome: String,
        difficulty: Int,
        language: String = "gu-IN",
    ) = ConceptEntity(
        id = id,
        subject = subject,
        standard = 2,
        language = language,
        titleGujarati = gujarati,
        titleEnglish = english,
        descriptionGujarati = description,
        difficulty = difficulty,
        expectedLearningOutcome = outcome,
        sortOrder = order,
        builtIn = true,
        bookId = null,
        chapterId = null,
        sourcePageStart = null,
        sourcePageEnd = null,
    )
}
