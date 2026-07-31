package com.mitra.learning.study

import com.mitra.learning.data.db.dao.BookDao
import com.mitra.learning.data.db.dao.ChapterDao
import com.mitra.learning.data.db.dao.PageKnowledgeDao
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus

class StudyContextService(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val pageKnowledgeDao: PageKnowledgeDao,
) {
    suspend fun findSources(question: String, limit: Int = 8): List<StudySource> {
        val books = bookDao.getAll().associateBy { it.id }
        val chapters = chapterDao.getAll()
            .filter { it.analysisStatus == ChapterAnalysisStatus.READY }
            .associateBy { it.id }
        val tokens = tokens(question)

        return pageKnowledgeDao.getAll()
            .mapNotNull { page ->
                val book = books[page.bookId] ?: return@mapNotNull null
                val chapter = chapters[page.chapterId] ?: return@mapNotNull null
                val text = listOfNotNull(
                    page.summaryGujarati,
                    page.visibleTextGujarati,
                    page.importantObjectsJson,
                    page.exercisesJson,
                    page.conceptsJson,
                ).joinToString("\n").take(4500)
                val score = score("${book.title} ${chapter.titleGujarati} ${chapter.titleEnglish.orEmpty()} $text", tokens)
                RankedSource(
                    score = score,
                    source = StudySource(
                        bookTitle = book.title,
                        chapterTitle = chapter.titleGujarati,
                        pageNumber = page.pageNumber,
                        text = text,
                    ),
                )
            }
            .sortedWith(compareByDescending<RankedSource> { it.score }.thenBy { it.source.pageNumber })
            .let { ranked ->
                val positive = ranked.filter { it.score > 0 }
                if (positive.isNotEmpty()) positive else ranked.take(4)
            }
            .take(limit.coerceIn(1, 10))
            .map { it.source }
    }

    suspend fun hasPreparedStudyMaterial(): Boolean =
        chapterDao.getAll().any { it.analysisStatus == ChapterAnalysisStatus.READY } &&
            pageKnowledgeDao.getAll().isNotEmpty()

    private fun score(text: String, tokens: Set<String>): Int {
        if (tokens.isEmpty()) return 0
        val haystack = normalize(text)
        return tokens.fold(0) { total, token ->
            total + when {
                haystack.contains(" $token ") -> 5
                haystack.contains(token) -> 2
                else -> 0
            }
        }
    }

    private fun tokens(value: String): Set<String> = normalize(value)
        .split(' ')
        .map { it.trim() }
        .filter { it.length >= 2 && it !in STOP_WORDS }
        .toSet()

    private fun normalize(value: String): String = " " + value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim() + " "

    private data class RankedSource(val score: Int, val source: StudySource)

    private companion object {
        val STOP_WORDS = setOf(
            "શું", "કે", "છે", "આ", "એ", "ને", "નો", "ની", "નું", "કેમ", "કયો", "કઈ",
            "what", "is", "are", "the", "a", "an", "this", "that", "and",
        )
    }
}
