package com.mitra.learning.ai.local

import com.mitra.learning.books.analysis.TocChapterSuggestion

private val tocLineWithLeaders = Regex(
    """^\s*(?:(\d{1,2})[.)\-:]?\s+)?(.{2,}?)\s*(?:[.…·•_-]{2,}|\s{2,})\s*(\d{1,3})\s*$"""
)
private val tocLineWithTrailingPage = Regex(
    """^\s*(?:(\d{1,2})[.)\-:]?\s+)?(.{3,}?)\s+(\d{1,3})\s*$"""
)
private val chapterNumberInTitle = Regex(
    """(?:પાઠ|અધ્યાય|chapter|lesson)\s*[-:]?\s*(\d{1,2})""",
    RegexOption.IGNORE_CASE,
)
private val administrativeWords = listOf(
    "વિષયસૂચિ", "અનુક્રમણિકા", "પ્રસ્તાવના", "આભાર",
    "contents", "index", "preface", "copyright",
)

/** Rule-based fallback used when no compatible LiteRT-LM model is installed. */
internal fun parseOfflineTocLine(rawLine: String, pageCount: Int): TocChapterSuggestion? {
    if (pageCount <= 0) return null
    val line = rawLine.toAsciiDigits().replace(Regex("\\s+"), " ").trim()
    if (line.length !in 4..180 || isOfflineAdministrativeText(line)) return null
    val match = tocLineWithLeaders.matchEntire(line)
        ?: tocLineWithTrailingPage.matchEntire(line)
        ?: return null
    val title = match.groupValues[2]
        .trim(' ', '.', '-', '–', '—', ':', '•')
        .replace(Regex("\\s+"), " ")
    val page = match.groupValues[3].toIntOrNull()?.takeIf { it in 1..pageCount } ?: return null
    if (title.length < 2 || title.all { it.isDigit() || it.isWhitespace() }) return null
    val chapterNumber = match.groupValues[1].toIntOrNull()
        ?: chapterNumberInTitle.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return TocChapterSuggestion(
        chapterNumber = chapterNumber,
        titleGujarati = title,
        startPage = page,
    )
}

internal fun isOfflineAdministrativeText(text: String): Boolean {
    val value = text.lowercase()
    return administrativeWords.any(value::contains)
}

private fun String.toAsciiDigits(): String = buildString(length) {
    for (char in this@toAsciiDigits) {
        append(
            when (char) {
                '૦' -> '0'
                '૧' -> '1'
                '૨' -> '2'
                '૩' -> '3'
                '૪' -> '4'
                '૫' -> '5'
                '૬' -> '6'
                '૭' -> '7'
                '૮' -> '8'
                '૯' -> '9'
                else -> char
            }
        )
    }
}
