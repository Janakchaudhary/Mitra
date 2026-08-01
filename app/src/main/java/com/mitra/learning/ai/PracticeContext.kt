package com.mitra.learning.ai

data class PracticeContext(
    val bookTitle: String? = null,
    val chapterTitleGujarati: String? = null,
    val groundedBookText: String? = null,
    val recentQuestionFingerprints: Set<String> = emptySet(),
)
