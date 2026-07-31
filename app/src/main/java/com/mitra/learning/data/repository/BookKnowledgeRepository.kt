package com.mitra.learning.data.repository

import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.PageKnowledgeEntity
import kotlinx.coroutines.flow.Flow

interface BookKnowledgeRepository {
    fun observeChapters(bookId: String): Flow<List<ChapterEntity>>
    suspend fun chaptersForBook(bookId: String): List<ChapterEntity>
    suspend fun getChapter(chapterId: String): ChapterEntity?
    suspend fun upsertChapter(chapter: ChapterEntity)
    suspend fun upsertChapters(chapters: List<ChapterEntity>)
    suspend fun deleteChapter(chapter: ChapterEntity)
    suspend fun setChapterStatus(chapterId: String, status: ChapterAnalysisStatus)

    suspend fun pageKnowledge(chapterId: String): List<PageKnowledgeEntity>
    suspend fun replacePageKnowledge(chapterId: String, pages: List<PageKnowledgeEntity>)

    suspend fun conceptsForChapter(chapterId: String): List<ConceptEntity>
    suspend fun replaceChapterConcepts(chapterId: String, concepts: List<ConceptEntity>)

    suspend fun deleteAllForBook(bookId: String)
}
