package com.mitra.learning.core

import android.content.Context
import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.ConfigurableAiGateway
import com.mitra.learning.ai.settings.AiSettingsRepository
import com.mitra.learning.books.analysis.BookPreparationService
import com.mitra.learning.books.pdf.AndroidPdfPageRenderer
import com.mitra.learning.data.db.MitraDatabase
import com.mitra.learning.data.repository.BookKnowledgeRepository
import com.mitra.learning.data.repository.BookRepository
import com.mitra.learning.data.repository.LearningRepository
import com.mitra.learning.data.repository.LocalBookKnowledgeRepository
import com.mitra.learning.data.repository.LocalBookRepository
import com.mitra.learning.data.repository.LocalLearningRepository
import com.mitra.learning.learning.engine.DefaultLearningEngine
import com.mitra.learning.learning.engine.LearningEngine
import com.mitra.learning.security.AndroidKeystoreSecretStore
import com.mitra.learning.security.ParentPinRepository
import com.mitra.learning.security.SecretStore
import com.mitra.learning.voice.AndroidSpeechInput
import com.mitra.learning.voice.AndroidSpeechOutput
import com.mitra.learning.voice.SpeechInput
import com.mitra.learning.voice.SpeechOutput

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: MitraDatabase = MitraDatabase.create(appContext)
    val pdfRenderer = AndroidPdfPageRenderer()

    val bookKnowledgeRepository: BookKnowledgeRepository = LocalBookKnowledgeRepository(
        chapterDao = database.chapterDao(),
        pageKnowledgeDao = database.pageKnowledgeDao(),
        conceptDao = database.conceptDao(),
    )

    val bookRepository: BookRepository = LocalBookRepository(
        context = appContext,
        bookDao = database.bookDao(),
        pdfRenderer = pdfRenderer,
        knowledgeRepository = bookKnowledgeRepository,
    )

    val learningRepository: LearningRepository = LocalLearningRepository(
        conceptDao = database.conceptDao(),
        masteryDao = database.masteryDao(),
        sessionDao = database.sessionDao(),
        attemptDao = database.attemptDao(),
    )

    val aiSettingsRepository = AiSettingsRepository(appContext)
    val secretStore: SecretStore = AndroidKeystoreSecretStore(appContext)
    val configurableAiGateway = ConfigurableAiGateway(
        settingsRepository = aiSettingsRepository,
        secretStore = secretStore,
    )
    val aiGateway: AiGateway = configurableAiGateway

    val bookPreparationService = BookPreparationService(
        bookRepository = bookRepository,
        knowledgeRepository = bookKnowledgeRepository,
        bookDao = database.bookDao(),
        pdfRenderer = pdfRenderer,
        aiGateway = aiGateway,
    )

    val speechInput: SpeechInput = AndroidSpeechInput(appContext)
    val speechOutput: SpeechOutput = AndroidSpeechOutput(appContext)

    val learningEngine: LearningEngine = DefaultLearningEngine(
        repository = learningRepository,
        aiGateway = aiGateway,
        bookKnowledgeRepository = bookKnowledgeRepository,
        bookRepository = bookRepository,
    )

    val parentPinRepository = ParentPinRepository(appContext)
}
