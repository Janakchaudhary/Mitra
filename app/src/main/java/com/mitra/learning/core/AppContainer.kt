package com.mitra.learning.core

import android.content.Context
import com.mitra.learning.ai.AiGateway
import com.mitra.learning.ai.ConfigurableAiGateway
import com.mitra.learning.ai.local.LiteRtLocalModel
import com.mitra.learning.ai.local.LocalModelStore
import com.mitra.learning.ai.local.OfflineAiGateway
import com.mitra.learning.ai.settings.AiSettingsRepository
import com.mitra.learning.books.analysis.BookPreparationService
import com.mitra.learning.books.importing.PreparedBookImportService
import com.mitra.learning.books.pdf.AndroidPdfPageRenderer
import com.mitra.learning.books.text.AndroidOfflinePageTextExtractor
import com.mitra.learning.books.text.TesseractOcrEngine
import com.mitra.learning.books.work.BookPreparationCoordinator
import com.mitra.learning.data.db.MitraDatabase
import com.mitra.learning.data.backup.MitraBackupService
import com.mitra.learning.data.repository.BookKnowledgeRepository
import com.mitra.learning.data.repository.BookRepository
import com.mitra.learning.data.repository.LearningRepository
import com.mitra.learning.data.repository.LocalBookKnowledgeRepository
import com.mitra.learning.data.repository.LocalBookRepository
import com.mitra.learning.data.repository.LocalLearningRepository
import com.mitra.learning.data.repository.LocalProgressRepository
import com.mitra.learning.data.repository.ProgressRepository
import com.mitra.learning.data.reset.AppDataResetService
import com.mitra.learning.learning.limits.LearningLimitService
import com.mitra.learning.learning.offline.OfflineQuestionBank
import com.mitra.learning.learning.assignment.ParentQuizRepository
import com.mitra.learning.learning.assignment.ParentQuizService
import com.mitra.learning.network.NetworkMonitor
import com.mitra.learning.security.ParentAccessManager
import com.mitra.learning.settings.LearningSettingsRepository
import com.mitra.learning.learning.engine.DefaultLearningEngine
import com.mitra.learning.learning.engine.LearningEngine
import com.mitra.learning.security.AndroidKeystoreSecretStore
import com.mitra.learning.security.ParentPinRepository
import com.mitra.learning.security.SecretStore
import com.mitra.learning.voice.AndroidSpeechInput
import com.mitra.learning.voice.AndroidSpeechOutput
import com.mitra.learning.voice.SpeechInput
import com.mitra.learning.voice.SpeechOutput
import com.mitra.learning.study.StudyContextService
import com.mitra.learning.study.practice.MitraVoicePracticeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: MitraDatabase = MitraDatabase.create(appContext)
    val pdfRenderer = AndroidPdfPageRenderer()
    val tesseractOcrEngine = TesseractOcrEngine(appContext)
    val offlinePageTextExtractor = AndroidOfflinePageTextExtractor(
        context = appContext,
        renderer = pdfRenderer,
        ocr = tesseractOcrEngine,
        rawPageTextDao = database.rawPageTextDao(),
    )

    val bookKnowledgeRepository: BookKnowledgeRepository = LocalBookKnowledgeRepository(
        chapterDao = database.chapterDao(),
        pageKnowledgeDao = database.pageKnowledgeDao(),
        conceptDao = database.conceptDao(),
        pageKnowledgeFtsDao = database.pageKnowledgeFtsDao(),
        vocabularyDao = database.vocabularyDao(),
        preparedQuestionDao = database.preparedQuestionDao(),
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

    val progressRepository: ProgressRepository = LocalProgressRepository(
        conceptDao = database.conceptDao(),
        masteryDao = database.masteryDao(),
        sessionDao = database.sessionDao(),
        attemptDao = database.attemptDao(),
    )

    val aiSettingsRepository = AiSettingsRepository(appContext)
    val learningSettingsRepository = LearningSettingsRepository(appContext)
    val parentAccessManager = ParentAccessManager()
    val networkMonitor = NetworkMonitor(appContext)
    val learningLimitService = LearningLimitService(learningSettingsRepository, database.sessionDao())
    val secretStore: SecretStore = AndroidKeystoreSecretStore(appContext)
    val localModelStore = LocalModelStore(appContext)
    val liteRtLocalModel = LiteRtLocalModel(appContext, localModelStore)
    val offlineAiGateway = OfflineAiGateway(liteRtLocalModel, localModelStore)
    val configurableAiGateway = ConfigurableAiGateway(
        settingsRepository = aiSettingsRepository,
        secretStore = secretStore,
        offline = offlineAiGateway,
    )
    val aiGateway: AiGateway = configurableAiGateway

    val offlineQuestionBank = OfflineQuestionBank(appContext)
    val preparedBookImportService = PreparedBookImportService(
        context = appContext,
        database = database,
        questionBank = offlineQuestionBank,
    )

    val bookPreparationCoordinator = BookPreparationCoordinator(
        context = appContext,
        jobDao = database.preparationJobDao(),
    )

    val bookPreparationService = BookPreparationService(
        bookRepository = bookRepository,
        knowledgeRepository = bookKnowledgeRepository,
        bookDao = database.bookDao(),
        pdfRenderer = pdfRenderer,
        aiGateway = aiGateway,
        pageTextExtractor = offlinePageTextExtractor,
        questionBank = offlineQuestionBank,
        preparedQuestionDao = database.preparedQuestionDao(),
        vocabularyDao = database.vocabularyDao(),
        database = database,
    )

    val speechInput: SpeechInput = AndroidSpeechInput(appContext)
    val speechOutput: SpeechOutput = AndroidSpeechOutput(appContext)
    val studyContextService = StudyContextService(
        bookDao = database.bookDao(),
        chapterDao = database.chapterDao(),
        pageKnowledgeDao = database.pageKnowledgeDao(),
        vocabularyDao = database.vocabularyDao(),
        pageKnowledgeFtsDao = database.pageKnowledgeFtsDao(),
    )
    val mitraVoicePracticeService = MitraVoicePracticeService(
        conceptDao = database.conceptDao(),
        chapterDao = database.chapterDao(),
        bookDao = database.bookDao(),
        pageKnowledgeDao = database.pageKnowledgeDao(),
        questionBank = offlineQuestionBank,
        preparedQuestionDao = database.preparedQuestionDao(),
        aiGateway = aiGateway,
    )

    val parentQuizRepository = ParentQuizRepository(appContext, database.parentQuizDao())
    val parentQuizService = ParentQuizService(
        practiceService = mitraVoicePracticeService,
        repository = parentQuizRepository,
        bookDao = database.bookDao(),
        chapterDao = database.chapterDao(),
        conceptDao = database.conceptDao(),
        preparedQuestionDao = database.preparedQuestionDao(),
        questionBank = offlineQuestionBank,
    )

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        appScope.launch {
            speechOutput.setStyle(learningSettingsRepository.get().voiceStyle)
        }
    }

    val learningEngine: LearningEngine = DefaultLearningEngine(
        repository = learningRepository,
        aiGateway = aiGateway,
        bookKnowledgeRepository = bookKnowledgeRepository,
        bookRepository = bookRepository,
        questionBank = offlineQuestionBank,
        preparedQuestionDao = database.preparedQuestionDao(),
    )

    val parentPinRepository = ParentPinRepository(appContext)

    val backupService = MitraBackupService(appContext, database, learningSettingsRepository)

    val dataResetService = AppDataResetService(
        context = appContext,
        database = database,
        bookKnowledgeRepository = bookKnowledgeRepository,
        parentPinRepository = parentPinRepository,
        aiSettingsRepository = aiSettingsRepository,
        learningSettingsRepository = learningSettingsRepository,
        secretStore = secretStore,
        questionBank = offlineQuestionBank,
    )
}
