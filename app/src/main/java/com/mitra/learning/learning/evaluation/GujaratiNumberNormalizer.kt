package com.mitra.learning.learning.evaluation

object GujaratiNumberNormalizer {
    private val gujaratiDigits = mapOf(
        '૦' to '0', '૧' to '1', '૨' to '2', '૩' to '3', '૪' to '4',
        '૫' to '5', '૬' to '6', '૭' to '7', '૮' to '8', '૯' to '9',
    )

    private val words = mapOf(
        "શૂન્ય" to 0,
        "એક" to 1,
        "બે" to 2,
        "ત્રણ" to 3,
        "ચાર" to 4,
        "પાંચ" to 5,
        "છ" to 6,
        "સાત" to 7,
        "આઠ" to 8,
        "નવ" to 9,
        "દસ" to 10,
        "અગિયાર" to 11,
        "બાર" to 12,
        "તેર" to 13,
        "ચૌદ" to 14,
        "પંદર" to 15,
        "સોળ" to 16,
        "સત્તર" to 17,
        "અઢાર" to 18,
        "ઓગણીસ" to 19,
        "વીસ" to 20,
    )

    fun normalizeDigits(text: String): String = buildString(text.length) {
        text.forEach { append(gujaratiDigits[it] ?: it) }
    }

    fun parseInt(text: String): Int? {
        val clean = text.trim().lowercase()
        words[clean]?.let { return it }
        return normalizeDigits(clean).toIntOrNull()
    }
}
