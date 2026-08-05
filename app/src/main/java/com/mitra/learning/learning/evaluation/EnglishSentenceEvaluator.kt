package com.mitra.learning.learning.evaluation

/**
 * Child-friendly sentence matching. It accepts harmless article differences and common
 * speech-recognition variants while still requiring the key subject/action/object words.
 */
object EnglishSentenceEvaluator {
    data class Result(
        val correct: Boolean,
        val missingWords: List<String> = emptyList(),
    )

    fun evaluate(expected: String, actual: String): Result {
        val expectedWords = normalize(expected)
        val actualWords = normalize(actual)
        if (expectedWords.isEmpty() || actualWords.isEmpty()) return Result(false, expectedWords)
        if (expectedWords == actualWords) return Result(true)

        val optional = setOf("a", "an", "the", "this", "that")
        val equivalents = mapOf(
            "is" to setOf("is", "s"),
            "are" to setOf("are", "re"),
            "children" to setOf("children", "kids"),
            "girl" to setOf("girl", "child"),
            "boy" to setOf("boy", "child"),
        )
        val actualSet = actualWords.toSet()
        val required = expectedWords.filterNot { it in optional }
        val missing = required.filter { word ->
            val accepted = equivalents[word] ?: setOf(word)
            accepted.none(actualSet::contains)
        }.distinct()

        // Require the important content words, but permit natural article/word-order variation.
        val coverage = if (required.isEmpty()) 0f else (required.size - missing.size).toFloat() / required.size
        return Result(correct = missing.isEmpty() || coverage >= 0.9f, missingWords = missing)
    }

    private fun normalize(value: String): List<String> = value
        .lowercase()
        .replace("'", "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
}
