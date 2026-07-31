package com.mitra.learning.books.analysis

object ChapterRangeResolver {
    fun fromStarts(
        suggestions: List<TocChapterSuggestion>,
        pageCount: Int,
    ): List<TocChapterSuggestionWithEnd> {
        if (pageCount <= 0) return emptyList()
        val normalized = suggestions
            .filter { it.startPage in 1..pageCount }
            .sortedBy { it.startPage }
            .distinctBy { it.startPage }

        return normalized.mapIndexed { index, item ->
            val nextStart = normalized.getOrNull(index + 1)?.startPage
            TocChapterSuggestionWithEnd(
                chapterNumber = item.chapterNumber,
                titleGujarati = item.titleGujarati.ifBlank { "પાઠ ${index + 1}" },
                titleEnglish = item.titleEnglish,
                startPage = item.startPage,
                endPage = ((nextStart ?: (pageCount + 1)) - 1).coerceIn(item.startPage, pageCount),
            )
        }
    }
}

data class TocChapterSuggestionWithEnd(
    val chapterNumber: Int?,
    val titleGujarati: String,
    val titleEnglish: String?,
    val startPage: Int,
    val endPage: Int,
)
