package com.mitra.learning.study.practice

data class SpellingWord(
    val word: String,
    val meaningGujarati: String,
)

object EnglishSpellingLexicon {
    val standard2Words: List<SpellingWord> = listOf(
        SpellingWord("cat", "બિલાડી"),
        SpellingWord("dog", "કૂતરો"),
        SpellingWord("sun", "સૂર્ય"),
        SpellingWord("moon", "ચંદ્ર"),
        SpellingWord("star", "તારો"),
        SpellingWord("book", "પુસ્તક"),
        SpellingWord("pen", "પેન"),
        SpellingWord("bag", "થેલો"),
        SpellingWord("ball", "દડો"),
        SpellingWord("tree", "ઝાડ"),
        SpellingWord("fish", "માછલી"),
        SpellingWord("bird", "પક્ષી"),
        SpellingWord("milk", "દૂધ"),
        SpellingWord("water", "પાણી"),
        SpellingWord("apple", "સફરજન"),
        SpellingWord("mango", "કેરી"),
        SpellingWord("green", "લીલો"),
        SpellingWord("red", "લાલ"),
        SpellingWord("blue", "વાદળી"),
        SpellingWord("school", "શાળા"),
        SpellingWord("mother", "માતા"),
        SpellingWord("father", "પિતા"),
        SpellingWord("happy", "ખુશ"),
        SpellingWord("small", "નાનું"),
        SpellingWord("house", "ઘર"),
    )

    fun find(raw: String): SpellingWord? {
        val normalized = raw.lowercase().replace(Regex("[^a-z]+"), " ").trim()
        return standard2Words.firstOrNull { word ->
            Regex("(^|\\s)${Regex.escape(word.word)}(\\s|$)").containsMatchIn(normalized)
        }
    }

    fun letters(word: String): String = word.uppercase().toCharArray().joinToString(" - ")
}
