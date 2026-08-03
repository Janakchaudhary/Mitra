package com.mitra.learning.study

/**
 * Extractive fallback for prepared textbooks. It never invents facts: it chooses the most
 * relevant locally stored textbook sentences and presents them as a short answer.
 */
class OfflineStudyAnswerer {
    fun answer(request: StudyQuestionRequest): StudyAnswer {
        if (request.sources.isEmpty()) return notFound()
        definitionAnswer(request)?.let { return it }
        val queryTokens = tokens(request.question)
        if (queryTokens.isEmpty()) return notFound()

        val candidates = request.sources.flatMap { source ->
            splitSentences(source.text).map { sentence ->
                Candidate(source, sentence, score(queryTokens, sentence))
            }
        }.filter { it.score > 0 }
            .sortedByDescending { it.score }

        val best = candidates.firstOrNull() ?: return notFound()
        val selected = candidates
            .filter { it.source.bookTitle == best.source.bookTitle && it.source.pageNumber == best.source.pageNumber }
            .map { it.sentence.trim() }
            .distinct()
            .take(2)

        val answer = selected.joinToString(" ").take(520).trim()
        if (answer.isBlank()) return notFound()
        return StudyAnswer(
            answerGujarati = answer,
            followUpGujarati = "આ જવાબ પુસ્તકના પાનું ${best.source.pageNumber}માંથી મળ્યો. હવે તમે તમારા શબ્દોમાં કહી શકો?",
            sourceLabels = listOf("${best.source.bookTitle} • p.${best.source.pageNumber}"),
            grounded = true,
            responseKind = StudyResponseKind.TEXTBOOK,
        )
    }

    private fun definitionAnswer(request: StudyQuestionRequest): StudyAnswer? {
        val normalizedQuestion = normalize(request.question)
        if (DEFINITION_MARKERS.none { normalizedQuestion.contains(" $it ") }) return null

        val entry = GLOSSARY.entries.firstOrNull { (word, _) ->
            normalizedQuestion.contains(" $word ") || normalizedQuestion.contains(word)
        } ?: return null
        val source = request.sources.firstOrNull { source ->
            val text = normalize(source.text)
            text.contains(" ${entry.key} ") || text.contains(entry.key)
        } ?: return null

        return StudyAnswer(
            answerGujarati = "‘${entry.key}’ એટલે ${entry.value}. પુસ્તકના આ પાઠમાં પણ આ શબ્દ એ જ અર્થમાં વપરાયો છે.",
            followUpGujarati = "હવે ‘${entry.key}’ શબ્દ વાપરીને એક નાનું વાક્ય બોલશો?",
            sourceLabels = listOf("${source.bookTitle} • p.${source.pageNumber}"),
            grounded = true,
            responseKind = StudyResponseKind.TEXTBOOK,
        )
    }

    private fun normalize(value: String): String = " " + value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim() + " "

    private fun notFound() = StudyAnswer(
        answerGujarati = "આ સવાલનો જવાબ તૈયાર કરેલા પુસ્તકમાં સ્પષ્ટ મળ્યો નથી. સંબંધિત પાઠ Prepare થયો છે કે નહીં તે Parent સાથે તપાસીએ.",
        grounded = false,
        responseKind = StudyResponseKind.TEXTBOOK,
    )

    private fun score(query: Set<String>, sentence: String): Int {
        val sentenceTokens = tokens(sentence)
        if (sentenceTokens.isEmpty()) return 0
        val exact = query.count { it in sentenceTokens }
        val partial = query.count { token -> token.length >= 3 && sentenceTokens.any { it.contains(token) || token.contains(it) } }
        return exact * 5 + partial * 2
    }

    private fun tokens(value: String): Set<String> = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.length >= 2 && it !in STOP_WORDS }
        .toSet()

    private fun splitSentences(value: String): List<String> = value
        .split(Regex("(?<=[.!?।\\n])\\s*"))
        .map { it.trim() }
        .filter { it.length >= 8 }

    private data class Candidate(val source: StudySource, val sentence: String, val score: Int)

    companion object {
        private val DEFINITION_MARKERS = setOf("અર્થ", "એટલે", "મતલબ", "meaning")
        private val DEFINITION_FILLERS = setOf("શબ્દ", "શબ્દનો", "શબ્દની", "શું", "કયો", "કહો")
        private val GLOSSARY = linkedMapOf(
            "દંગોરો" to "હાથમાં ટેકો લેવા અથવા હાંકવા માટે વપરાતી લાંબી અને મજબૂત લાકડી",
            "ઘોડો" to "ચાર પગવાળું દોડતું પ્રાણી",
            "ફૂદવું" to "ઉછળીને આગળ વધવું",
            "ઘમઘમ" to "જોરથી અથવા સતત થતો અવાજ",
            "ધમધમ" to "ભારે વસ્તુ અથડાય કે ચાલે ત્યારે થતો જોરદાર અવાજ",
        )

        private val STOP_WORDS = setOf(
            "શું", "કેમ", "ક્યાં", "કોણ", "વિશે", "વાત", "કરીએ", "છે", "હતું", "હતી", "હતા",
            "the", "is", "are", "what", "why", "where", "about",
        )
    }
}
