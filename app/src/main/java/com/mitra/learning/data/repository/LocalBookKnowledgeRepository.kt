package com.mitra.learning.data.repository

import com.mitra.learning.data.db.dao.ChapterDao
import com.mitra.learning.data.db.dao.ConceptDao
import com.mitra.learning.data.db.dao.PageKnowledgeDao
import com.mitra.learning.data.db.dao.PageKnowledgeFtsDao
import com.mitra.learning.data.db.dao.PreparedQuestionDao
import com.mitra.learning.data.db.dao.VocabularyDao
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.PageKnowledgeEntity
import com.mitra.learning.data.db.entity.PageKnowledgeFtsEntity
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow

class LocalBookKnowledgeRepository(
    private val chapterDao: ChapterDao,
    private val pageKnowledgeDao: PageKnowledgeDao,
    private val conceptDao: ConceptDao,
    private val pageKnowledgeFtsDao: PageKnowledgeFtsDao? = null,
    private val vocabularyDao: VocabularyDao? = null,
    private val preparedQuestionDao: PreparedQuestionDao? = null,
) : BookKnowledgeRepository {
    override fun observeChapters(bookId: String): Flow<List<ChapterEntity>> = chapterDao.observeForBook(bookId)
    override suspend fun chaptersForBook(bookId: String): List<ChapterEntity> = chapterDao.forBook(bookId)
    override suspend fun getChapter(chapterId: String): ChapterEntity? = chapterDao.findById(chapterId)
    override suspend fun upsertChapter(chapter: ChapterEntity) = chapterDao.upsert(chapter)
    override suspend fun upsertChapters(chapters: List<ChapterEntity>) = chapterDao.upsertAll(chapters)

    override suspend fun deleteChapter(chapter: ChapterEntity) {
        pageKnowledgeFtsDao?.deleteForChapter(chapter.id)
        pageKnowledgeDao.deleteForChapter(chapter.id)
        vocabularyDao?.deleteForChapter(chapter.id)
        preparedQuestionDao?.deleteForChapter(chapter.id)
        conceptDao.deleteForChapter(chapter.id)
        chapterDao.delete(chapter)
    }

    override suspend fun setChapterStatus(chapterId: String, status: ChapterAnalysisStatus) =
        chapterDao.updateStatus(chapterId, status)

    override suspend fun pageKnowledge(chapterId: String): List<PageKnowledgeEntity> =
        pageKnowledgeDao.forChapter(chapterId)

    override suspend fun replacePageKnowledge(chapterId: String, pages: List<PageKnowledgeEntity>) {
        pageKnowledgeFtsDao?.deleteForChapter(chapterId)
        pageKnowledgeDao.deleteForChapter(chapterId)
        pageKnowledgeDao.upsertAll(pages)
        pageKnowledgeFtsDao?.upsertAll(pages.map(PageKnowledgeEntity::toFts))
    }

    override suspend fun conceptsForChapter(chapterId: String): List<ConceptEntity> = conceptDao.forChapter(chapterId)

    override suspend fun replaceChapterConcepts(chapterId: String, concepts: List<ConceptEntity>) {
        conceptDao.deleteForChapter(chapterId)
        conceptDao.upsertAll(concepts)
    }

    override suspend fun setConceptPracticeReady(conceptId: String, ready: Boolean) =
        conceptDao.updatePracticeReady(conceptId, ready)

    override suspend fun deleteAllForBook(bookId: String) {
        pageKnowledgeFtsDao?.deleteForBook(bookId)
        pageKnowledgeDao.deleteForBook(bookId)
        vocabularyDao?.deleteForBook(bookId)
        preparedQuestionDao?.deleteForBook(bookId)
        conceptDao.deleteForBook(bookId)
        chapterDao.deleteForBook(bookId)
    }

    private fun PageKnowledgeEntity.toFts(): PageKnowledgeFtsEntity = PageKnowledgeFtsEntity(
        rowId = stableRowId(id),
        pageKnowledgeId = id,
        bookId = bookId,
        chapterId = chapterId,
        pageNumberText = pageNumber.toString(),
        content = listOfNotNull(
            summaryGujarati,
            visibleTextGujarati,
            importantObjectsJson,
            exercisesJson,
            conceptsJson,
        ).joinToString("\n"),
    )

    private fun stableRowId(value: String): Long {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return ByteBuffer.wrap(bytes.copyOfRange(0, 8)).long and Long.MAX_VALUE
    }
}
