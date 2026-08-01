package com.mitra.learning.data.repository

import com.mitra.learning.data.db.dao.ChapterDao
import com.mitra.learning.data.db.dao.ConceptDao
import com.mitra.learning.data.db.dao.PageKnowledgeDao
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.PageKnowledgeEntity
import kotlinx.coroutines.flow.Flow

class LocalBookKnowledgeRepository(
    private val chapterDao: ChapterDao,
    private val pageKnowledgeDao: PageKnowledgeDao,
    private val conceptDao: ConceptDao,
) : BookKnowledgeRepository {
    override fun observeChapters(bookId: String): Flow<List<ChapterEntity>> = chapterDao.observeForBook(bookId)
    override suspend fun chaptersForBook(bookId: String): List<ChapterEntity> = chapterDao.forBook(bookId)
    override suspend fun getChapter(chapterId: String): ChapterEntity? = chapterDao.findById(chapterId)
    override suspend fun upsertChapter(chapter: ChapterEntity) = chapterDao.upsert(chapter)
    override suspend fun upsertChapters(chapters: List<ChapterEntity>) = chapterDao.upsertAll(chapters)

    override suspend fun deleteChapter(chapter: ChapterEntity) {
        pageKnowledgeDao.deleteForChapter(chapter.id)
        conceptDao.deleteForChapter(chapter.id)
        chapterDao.delete(chapter)
    }

    override suspend fun setChapterStatus(chapterId: String, status: ChapterAnalysisStatus) =
        chapterDao.updateStatus(chapterId, status)

    override suspend fun pageKnowledge(chapterId: String): List<PageKnowledgeEntity> =
        pageKnowledgeDao.forChapter(chapterId)

    override suspend fun replacePageKnowledge(chapterId: String, pages: List<PageKnowledgeEntity>) {
        pageKnowledgeDao.deleteForChapter(chapterId)
        pageKnowledgeDao.upsertAll(pages)
    }

    override suspend fun conceptsForChapter(chapterId: String): List<ConceptEntity> = conceptDao.forChapter(chapterId)

    override suspend fun replaceChapterConcepts(chapterId: String, concepts: List<ConceptEntity>) {
        conceptDao.deleteForChapter(chapterId)
        conceptDao.upsertAll(concepts)
    }

    override suspend fun setConceptPracticeReady(conceptId: String, ready: Boolean) =
        conceptDao.updatePracticeReady(conceptId, ready)

    override suspend fun deleteAllForBook(bookId: String) {
        pageKnowledgeDao.deleteForBook(bookId)
        conceptDao.deleteForBook(bookId)
        chapterDao.deleteForBook(bookId)
    }
}
