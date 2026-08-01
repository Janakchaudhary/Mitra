package com.mitra.learning.learning.evaluation

/**
 * Local numeric answer normalizer for Standard 2.
 * Supports Gujarati/Latin digits, Gujarati number words through 100, and simple English words.
 */
object GujaratiNumberNormalizer {
    private val gujaratiDigits = mapOf(
        '૦' to '0', '૧' to '1', '૨' to '2', '૩' to '3', '૪' to '4',
        '૫' to '5', '૬' to '6', '૭' to '7', '૮' to '8', '૯' to '9',
    )

    private val words: Map<String, Int> = buildMap {
        fun n(value: Int, vararg spellings: String) = spellings.forEach { put(it, value) }

        n(0, "શૂન્ય")
        n(1, "એક")
        n(2, "બે")
        n(3, "ત્રણ")
        n(4, "ચાર")
        n(5, "પાંચ")
        n(6, "છ")
        n(7, "સાત")
        n(8, "આઠ")
        n(9, "નવ")
        n(10, "દસ", "દશ")
        n(11, "અગિયાર")
        n(12, "બાર")
        n(13, "તેર")
        n(14, "ચૌદ")
        n(15, "પંદર")
        n(16, "સોળ")
        n(17, "સત્તર")
        n(18, "અઢાર")
        n(19, "ઓગણીસ", "ઓગણિસ")
        n(20, "વીસ")
        n(21, "એકવીસ")
        n(22, "બાવીસ")
        n(23, "તેવીસ", "ત્રેવીસ")
        n(24, "ચોવીસ")
        n(25, "પચ્ચીસ")
        n(26, "છવીસ")
        n(27, "સત્તાવીસ")
        n(28, "અઠ્ઠાવીસ")
        n(29, "ઓગણત્રીસ")
        n(30, "ત્રીસ")
        n(31, "એકત્રીસ")
        n(32, "બત્રીસ")
        n(33, "તેત્રીસ", "ત્રેત્રીસ")
        n(34, "ચોત્રીસ")
        n(35, "પાંત્રીસ")
        n(36, "છત્રીસ")
        n(37, "સાડત્રીસ", "સડત્રીસ")
        n(38, "આડત્રીસ", "અડત્રીસ")
        n(39, "ઓગણચાલીસ")
        n(40, "ચાલીસ")
        n(41, "એકતાલીસ")
        n(42, "બેતાલીસ")
        n(43, "ત્રેતાલીસ", "તેતાલીસ")
        n(44, "ચુંમાલીસ", "ચુમાલીસ")
        n(45, "પિસ્તાલીસ")
        n(46, "છેતાલીસ")
        n(47, "સુડતાલીસ")
        n(48, "અડતાલીસ")
        n(49, "ઓગણપચાસ")
        n(50, "પચાસ")
        n(51, "એકાવન")
        n(52, "બાવન")
        n(53, "ત્રેપન")
        n(54, "ચોપન")
        n(55, "પંચાવન")
        n(56, "છપ્પન")
        n(57, "સત્તાવન")
        n(58, "અઠ્ઠાવન")
        n(59, "ઓગણસાઠ")
        n(60, "સાઈઠ", "સાઠ")
        n(61, "એકસઠ")
        n(62, "બાસઠ")
        n(63, "ત્રેસઠ")
        n(64, "ચોસઠ")
        n(65, "પાંસઠ")
        n(66, "છાસઠ")
        n(67, "સડસઠ")
        n(68, "અડસઠ")
        n(69, "અગણોસિત્તેર", "ઓગણસિત્તેર")
        n(70, "સિત્તેર")
        n(71, "એકોતેર")
        n(72, "બોતેર")
        n(73, "તોતેર", "ત્રોતેર")
        n(74, "ચુમોતેર")
        n(75, "પંચોતેર")
        n(76, "છોતેર")
        n(77, "સિત્યોતેર", "સિત્તોતેર")
        n(78, "ઇઠ્યોતેર", "ઈઠ્યોતેર", "અઠ્યોતેર")
        n(79, "ઓગણાએંસી", "ઓગણએંસી")
        n(80, "એંસી")
        n(81, "એક્યાસી")
        n(82, "બ્યાસી")
        n(83, "ત્યાસી", "ત્ર્યાસી")
        n(84, "ચોર્યાસી")
        n(85, "પંચાસી", "પંચ્યાસી")
        n(86, "છ્યાસી")
        n(87, "સિત્યાસી")
        n(88, "ઈઠ્યાસી", "ઇઠ્યાસી", "અઠ્યાસી")
        n(89, "નેવ્યાસી")
        n(90, "નેવું")
        n(91, "એકાણું")
        n(92, "બાણું")
        n(93, "ત્રાણું")
        n(94, "ચોરાણું")
        n(95, "પંચાણું")
        n(96, "છન્નું")
        n(97, "સત્તાણું")
        n(98, "અઠ્ઠાણું")
        n(99, "નવ્વાણું")
        n(100, "સો", "એકસો", "એક સો")
    }

    private val englishUnits = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
        "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
        "eighteen" to 18, "nineteen" to 19,
    )

    private val englishTens = mapOf(
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
        "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90,
    )

    fun normalizeDigits(text: String): String = buildString(text.length) {
        text.forEach { append(gujaratiDigits[it] ?: it) }
    }



    /** Extracts numbers from a spoken or typed expression while preserving order. */
    fun extractInts(text: String, maxCount: Int = 8): List<Int> {
        if (maxCount <= 0) return emptyList()
        val clean = normalizeDigits(text.lowercase()).replace('-', ' ')
        val tokens = Regex("[\\p{L}\\p{M}\\d]+")
            .findAll(clean)
            .map { it.value }
            .toList()
        val values = mutableListOf<Int>()
        var index = 0
        while (index < tokens.size && values.size < maxCount) {
            val token = tokens[index]
            val numeric = token.toIntOrNull()
            if (numeric != null) {
                values += numeric
                index += 1
                continue
            }

            if (token == "એક" && tokens.getOrNull(index + 1) == "સો") {
                values += 100
                index += 2
                continue
            }
            val gujarati = words[token]
            if (gujarati != null) {
                values += gujarati
                index += 1
                continue
            }

            if (token == "one" && tokens.getOrNull(index + 1) == "hundred") {
                values += 100
                index += 2
                continue
            }
            val tens = englishTens[token]
            if (tens != null) {
                val unit = tokens.getOrNull(index + 1)?.let(englishUnits::get)
                if (unit != null && unit in 1..9) {
                    values += tens + unit
                    index += 2
                } else {
                    values += tens
                    index += 1
                }
                continue
            }
            val english = englishUnits[token]
            if (english != null) {
                values += english
                index += 1
                continue
            }
            index += 1
        }
        return values
    }

    fun toGujaratiDigits(number: Int): String = number.toString().map { digit ->
        when (digit) {
            '0' -> '૦'; '1' -> '૧'; '2' -> '૨'; '3' -> '૩'; '4' -> '૪'
            '5' -> '૫'; '6' -> '૬'; '7' -> '૭'; '8' -> '૮'; '9' -> '૯'
            else -> digit
        }
    }.joinToString("")

    fun parseInt(text: String): Int? {
        val clean = text.trim().lowercase()
            .replace(Regex("[,.!?।॥]+$"), "")
            .trim()
        words[clean]?.let { return it }

        val numeric = normalizeDigits(clean)
            .replace(",", "")
            .trim()
            .toIntOrNull()
        if (numeric != null) return numeric

        return parseEnglishNumber(clean)
    }

    private fun parseEnglishNumber(text: String): Int? {
        val clean = text.replace('-', ' ').replace(Regex("\\s+"), " ").trim()
        if (clean == "one hundred" || clean == "hundred") return 100
        englishUnits[clean]?.let { return it }
        englishTens[clean]?.let { return it }
        val parts = clean.split(' ')
        if (parts.size == 2) {
            val tens = englishTens[parts[0]] ?: return null
            val unit = englishUnits[parts[1]]?.takeIf { it in 1..9 } ?: return null
            return tens + unit
        }
        return null
    }
}
