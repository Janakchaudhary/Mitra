package com.mitra.learning.study.practice

import com.mitra.learning.learning.evaluation.GujaratiNumberNormalizer

object SpokenAnswerNormalizer {
    fun numeric(value: String): Int? = GujaratiNumberNormalizer.parseInt(value)
        ?: GujaratiNumberNormalizer.extractInts(value, maxCount = 1).firstOrNull()

    fun text(value: String): String = value
        .lowercase()
        .replace(Regex("[\\p{Punct}।॥]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    /**
     * Android speech recognition may return either "cat", "c a t", or spoken letter names.
     * Convert all common English/Gujarati letter-name forms into one comparable word.
     */
    fun spelling(value: String): String {
        val clean = text(value)
        if (clean.isBlank()) return ""
        val tokens = clean.split(' ').filter(String::isNotBlank)
        if (tokens.size == 1) {
            val word = tokens.first().filter(Char::isLetter)
            if (word.length in 2..16 && word.all { it.code < 128 }) return word
        }
        if (tokens.size >= 2 && tokens.all { it.length == 1 && it.first().code < 128 }) {
            return tokens.joinToString("")
        }

        val mapped = tokens.mapNotNull { token -> LETTER_NAMES[token] }
        if (mapped.size == tokens.size && mapped.isNotEmpty()) return mapped.joinToString("")

        return clean.replace(" ", "").filter(Char::isLetter)
    }

    private val LETTER_NAMES: Map<String, String> = mapOf(
        "a" to "a", "ay" to "a", "એ" to "a",
        "b" to "b", "bee" to "b", "બી" to "b",
        "c" to "c", "see" to "c", "sea" to "c", "સી" to "c",
        "d" to "d", "dee" to "d", "ડી" to "d",
        "e" to "e", "ee" to "e", "ઈ" to "e",
        "f" to "f", "ef" to "f", "એફ" to "f",
        "g" to "g", "gee" to "g", "જી" to "g",
        "h" to "h", "aitch" to "h", "એચ" to "h",
        "i" to "i", "eye" to "i", "આઈ" to "i",
        "j" to "j", "jay" to "j", "જે" to "j",
        "k" to "k", "kay" to "k", "કે" to "k",
        "l" to "l", "el" to "l", "એલ" to "l",
        "m" to "m", "em" to "m", "એમ" to "m",
        "n" to "n", "en" to "n", "એન" to "n",
        "o" to "o", "oh" to "o", "ઓ" to "o",
        "p" to "p", "pee" to "p", "પી" to "p",
        "q" to "q", "cue" to "q", "ક્યુ" to "q",
        "r" to "r", "are" to "r", "આર" to "r",
        "s" to "s", "ess" to "s", "એસ" to "s",
        "t" to "t", "tee" to "t", "ટી" to "t",
        "u" to "u", "you" to "u", "યુ" to "u",
        "v" to "v", "vee" to "v", "વી" to "v",
        "w" to "w", "doubleyou" to "w", "ડબલ્યુ" to "w",
        "x" to "x", "ex" to "x", "એક્સ" to "x",
        "y" to "y", "why" to "y", "વાય" to "y",
        "z" to "z", "zee" to "z", "zed" to "z", "ઝેડ" to "z",
    )
}
