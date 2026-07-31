package com.mitra.learning.data.reset

import android.content.Context
import com.mitra.learning.ai.settings.AiSettingsRepository
import com.mitra.learning.data.db.MitraDatabase
import com.mitra.learning.data.db.entity.BookAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.repository.BookKnowledgeRepository
import com.mitra.learning.security.AndroidKeystoreSecretStore
import com.mitra.learning.security.ParentPinRepository
import com.mitra.learning.security.SecretStore
import com.mitra.learning.settings.LearningSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppDataResetService(
    private val context: Context,
    private val database: MitraDatabase,
    private val bookKnowledgeRepository: BookKnowledgeRepository,
    private val parentPinRepository: ParentPinRepository,
    private val aiSettingsRepository: AiSettingsRepository,
    private val learningSettingsRepository: LearningSettingsRepository,
    private val secretStore: SecretStore,
) {
    suspend fun resetLearningProgress() = withContext(Dispatchers.IO) {
        database.attemptDao().deleteAll()
        database.masteryDao().deleteAll()
        database.sessionDao().deleteAll()
    }

    suspend fun resetBookAnalysis() = withContext(Dispatchers.IO) {
        database.bookDao().getAll().forEach { book ->
            bookKnowledgeRepository.chaptersForBook(book.id).forEach { chapter ->
                bookKnowledgeRepository.replacePageKnowledge(chapter.id, emptyList())
                bookKnowledgeRepository.replaceChapterConcepts(chapter.id, emptyList())
                bookKnowledgeRepository.setChapterStatus(chapter.id, ChapterAnalysisStatus.NOT_PREPARED)
            }
            database.bookDao().update(book.copy(analysisStatus = BookAnalysisStatus.NOT_ANALYZED))
        }
    }

    suspend fun resetEverything() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        File(context.filesDir, "books").deleteRecursively()
        secretStore.removeSecret(AndroidKeystoreSecretStore.OPENAI_API_KEY)
        aiSettingsRepository.reset()
        learningSettingsRepository.reset()
        parentPinRepository.clear()
    }
}
