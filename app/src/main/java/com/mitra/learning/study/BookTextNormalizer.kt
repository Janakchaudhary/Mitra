package com.mitra.learning.study

object BookTextNormalizer {
    fun normalizeWord(value: String): String = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), "")
        .trim()

    fun queryWords(value: String): List<String> = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
        .split(Regex("\\s+"))
        .map(::normalizeWord)
        .filter { it.length >= 2 && it !in STOP_WORDS }
        .distinct()

    fun ftsQuery(value: String): String = queryWords(value)
        .take(8)
        .joinToString(" OR ") { token -> "${token.replace("\"", "") }*" }

    private val STOP_WORDS = setOf(
        "શું", "એટલે", "અર્થ", "મતલબ", "શબ્દ", "શબ્દનો", "શબ્દની", "કયો", "કઈ",
        "છે", "નો", "ની", "નું", "ને", "આ", "the", "what", "meaning", "is", "of",
    )
}
