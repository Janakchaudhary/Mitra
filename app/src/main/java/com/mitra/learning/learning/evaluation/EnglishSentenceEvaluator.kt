package com.mitra.learning.learning.evaluation

/**
 * Child-friendly grammar-aware sentence matching. Articles may vary naturally, but important
 * subject/action/object words and their order must still form a valid Standard 2 sentence.
 */
object EnglishSentenceEvaluator {
    data class Result(
        val correct: Boolean,
        val missingWords: List<String> = emptyList(),
        val orderIssue: Boolean = false,
        val grammarHint: String? = null,
    )

    fun evaluate(expected: String, actual: String): Result {
        val expectedWords = normalize(expected)
        val actualWords = normalize(actual)
        if (expectedWords.isEmpty() || actualWords.isEmpty()) return Result(false, expectedWords)
        if (expectedWords == actualWords) return Result(true)

        val optionalArticles = setOf("a", "an", "the")
        val expectedRequired = expectedWords.filterNot { it in optionalArticles }
        val actualCanonical = actualWords.map(::canonical)
        val missing = expectedRequired.filter { word -> canonical(word) !in actualCanonical }.distinct()
        if (missing.isNotEmpty()) {
            return Result(
                correct = false,
                missingWords = missing,
                grammarHint = when {
                    "is" in missing || "are" in missing -> "English sentence માં is/are ઉમેરો."
                    else -> null
                },
            )
        }

        val requiredCanonical = expectedRequired.map(::canonical)
        val ordered = isSubsequence(requiredCanonical, actualCanonical)
        if (!ordered) {
            return Result(
                correct = false,
                orderIssue = true,
                grammarHint = "શબ્દો સાચા છે, હવે subject પછી is/are અને પછી action/object ગોઠવો.",
            )
        }

        // This/that carry meaning and should not be silently swapped, but a/an/the may vary.
        val demonstratives = expectedWords.filter { it == "this" || it == "that" }
        val wrongDemonstrative = demonstratives.any { it !in actualCanonical }
        return if (wrongDemonstrative) {
            Result(false, missingWords = demonstratives, grammarHint = "નજીક માટે this અને દૂર માટે that વાપરો.")
        } else {
            Result(true)
        }
    }

    private fun isSubsequence(expected: List<String>, actual: List<String>): Boolean {
        var position = 0
        for (word in actual) {
            if (position < expected.size && expected[position] == word) position += 1
        }
        return position == expected.size
    }

    private fun canonical(word: String): String = when (word) {
        "kids" -> "children"
        "s" -> "is"
        "re" -> "are"
        else -> word
    }

    private fun normalize(value: String): List<String> = value
        .lowercase()
        .replace("'", "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
}
