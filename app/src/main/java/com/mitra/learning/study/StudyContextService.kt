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
            // Never feed unrelated first pages to the answerer. An empty result is safer and
            // lets Mitra clearly say that this prepared chapter does not contain the requested word.
            .filter { it.score > 0 }
            .take(limit.coerceIn(1, 10))
            .map { it.source }
    }

    suspend fun hasPreparedStudyMaterial(): Boolean =
        chapterDao.getAll().any { it.analysisStatus == ChapterAnalysisStatus.READY } &&
            pageKnowledgeDao.getAll().isNotEmpty()

    private fun score(text: String, tokens: Set<String>): Int {
        if (tokens.isEmpty()) return 0
        val haystack = normalize(text)
        val words = haystack.trim().split(' ').filter(String::isNotBlank)
        return tokens.fold(0) { total, token ->
            val stem = gujaratiStem(token)
            total + when {
                haystack.contains(" $token ") -> 8
                stem.length >= 3 && words.any { gujaratiStem(it) == stem } -> 5
                haystack.contains(token) -> 3
                token.length >= 4 && words.any { word -> editDistanceAtMostOne(token, word) } -> 2
                else -> 0
            }
        }
    }

    private fun gujaratiStem(value: String): String {
        var result = value
        listOf("માંથી", "વાળો", "વાળી", "વાળું", "નો", "ની", "નું", "ને", "માં", "થી").forEach { suffix ->
            if (result.length > suffix.length + 2 && result.endsWith(suffix)) result = result.dropLast(suffix.length)
        }
        return result
    }

    private fun editDistanceAtMostOne(left: String, right: String): Boolean {
        if (kotlin.math.abs(left.length - right.length) > 1) return false
        if (left == right) return true
        var i = 0
        var j = 0
        var edits = 0
        while (i < left.length && j < right.length) {
            if (left[i] == right[j]) { i += 1; j += 1; continue }
            edits += 1
            if (edits > 1) return false
            when {
                left.length > right.length -> i += 1
                right.length > left.length -> j += 1
                else -> { i += 1; j += 1 }
            }
        }
        return edits + (left.length - i) + (right.length - j) <= 1
    }

    private fun tokens(value: String): Set<String> = normalize(value)
        .split(' ')
        .map { it.trim() }
        .filter { it.length >= 2 && it !in STOP_WORDS }
        .toSet()

    private fun normalize(value: String): String = " " + value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
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
