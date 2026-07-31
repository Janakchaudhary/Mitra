package com.mitra.learning.study

data class StudySource(
    val bookTitle: String,
    val chapterTitle: String,
    val pageNumber: Int,
    val text: String,
)

data class StudyQuestionRequest(
    val question: String,
    val sources: List<StudySource>,
    val recentTurns: List<StudyChatTurn> = emptyList(),
)

data class StudyAnswer(
    val answerGujarati: String,
    val followUpGujarati: String? = null,
    val sourceLabels: List<String> = emptyList(),
    val grounded: Boolean = true,
)

data class StudyChatTurn(
    val question: String,
    val answer: String,
)
