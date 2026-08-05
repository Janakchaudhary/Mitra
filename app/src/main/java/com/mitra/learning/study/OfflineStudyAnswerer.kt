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

        request.sources.forEach { source ->
            val match = VOCABULARY_BLOCK.find(source.text) ?: return@forEach
            val word = match.groupValues[1].trim()
            val meaning = match.groupValues[2].trim()
            val simple = SIMPLE_BLOCK.find(source.text)?.groupValues?.get(1)?.trim()
            val example = EXAMPLE_BLOCK.find(source.text)?.groupValues?.get(1)?.trim()
            val askedTokens = tokens(request.question).map(::gujaratiStem)
            if (askedTokens.none { token -> token == gujaratiStem(word) || token.contains(gujaratiStem(word)) }) return@forEach
            val explanation = listOfNotNull(
                "‘$word’ એટલે $meaning.",
                simple?.takeIf(String::isNotBlank),
                example?.takeIf(String::isNotBlank)?.let { "ઉદાહરણ: $it" },
            ).joinToString(" ")
            return StudyAnswer(
                answerGujarati = explanation,
                followUpGujarati = "હવે ‘$word’ શબ્દ વાપરીને એક નાનું વાક્ય બોલશો?",
                sourceLabels = listOf("${source.bookTitle} • p.${source.pageNumber}"),
                grounded = true,
                responseKind = StudyResponseKind.TEXTBOOK,
            )
        }
        return null
    }

    private fun gujaratiStem(value: String): String {
        var result = value.lowercase()
        listOf("માંથી", "વાળો", "વાળી", "વાળું", "શબ્દનો", "શબ્દની", "શબ્દનું", "નો", "ની", "નું", "ને", "માં", "થી").forEach { suffix ->
            if (result.length > suffix.length + 1 && result.endsWith(suffix)) result = result.dropLast(suffix.length)
        }
        return result
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
        private val VOCABULARY_BLOCK = Regex("""શબ્દ\s*:\s*([^\n]+)\nઅર્થ\s*:\s*([^\n]+)""")
        private val SIMPLE_BLOCK = Regex("""સરળ સમજ\s*:\s*([^\n]+)""")
        private val EXAMPLE_BLOCK = Regex("""ઉદાહરણ\s*:\s*([^\n]+)""")

        private val STOP_WORDS = setOf(
            "શું", "કેમ", "ક્યાં", "કોણ", "વિશે", "વાત", "કરીએ", "છે", "હતું", "હતી", "હતા",
            "the", "is", "are", "what", "why", "where", "about",
        )
    }
}
