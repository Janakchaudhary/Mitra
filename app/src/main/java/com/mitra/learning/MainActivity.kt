package com.mitra.learning

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mitra.learning.core.AppContainer
import com.mitra.learning.ui.books.AddBookScreen
import com.mitra.learning.ui.books.AddBookViewModel
import com.mitra.learning.ui.books.BookDetailScreen
import com.mitra.learning.ui.books.BookDetailViewModel
import com.mitra.learning.ui.books.BookListScreen
import com.mitra.learning.ui.books.BookListViewModel
import com.mitra.learning.ui.books.BookSetupScreen
import com.mitra.learning.ui.books.BookSetupViewModel
import com.mitra.learning.ui.books.PdfViewerScreen
import com.mitra.learning.ui.books.PdfViewerViewModel
import com.mitra.learning.ui.activity.ActivityHubScreen
import com.mitra.learning.ui.activity.ColorLabScreen
import com.mitra.learning.ui.activity.SentenceBuilderScreen
import com.mitra.learning.ui.child.ChildBookListScreen
import com.mitra.learning.ui.child.ChildHomeScreen
import com.mitra.learning.ui.child.ChildHomeViewModel
import com.mitra.learning.ui.common.simpleViewModelFactory
import com.mitra.learning.ui.learning.LearningSessionScreen
import com.mitra.learning.ui.learning.LearningSessionViewModel
import com.mitra.learning.ui.parent.AiSettingsScreen
import com.mitra.learning.ui.parent.AiSettingsViewModel
import com.mitra.learning.ui.parent.ParentHomeScreen
import com.mitra.learning.ui.parent.ParentPinScreen
import com.mitra.learning.ui.parent.ParentPinViewModel
import com.mitra.learning.ui.progress.ProgressScreen
import com.mitra.learning.ui.progress.ProgressViewModel
import com.mitra.learning.ui.practice.PracticePickerScreen
import com.mitra.learning.ui.practice.PracticePickerViewModel
import com.mitra.learning.ui.settings.ParentSettingsScreen
import com.mitra.learning.ui.settings.ParentSettingsViewModel
import com.mitra.learning.ui.setup.SetupPinScreen
import com.mitra.learning.ui.setup.SetupPinViewModel
import com.mitra.learning.ui.study.StudyChatScreen
import com.mitra.learning.ui.study.StudyChatViewModel
import com.mitra.learning.ui.theme.MitraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var appReady = false
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !appReady }
        splashScreen.setOnExitAnimationListener { provider ->
            provider.view
                .animate()
                .alpha(0f)
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(220L)
                .withEndAction { provider.remove() }
                .start()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = LocalContext.current.applicationContext as MitraApplication
            MitraTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    MitraNav(
                        container = app.container,
                        onReady = { appReady = true },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as MitraApplication).container.parentAccessManager.onAppForegrounded()
    }

    override fun onStop() {
        (application as MitraApplication).container.parentAccessManager.onAppBackgrounded()
        super.onStop()
    }
}

private object Routes {
    const val Setup = "setup"
    const val Child = "child"
    const val ChildBooks = "child/books"
    const val Learning = "child/learning"
    const val LearningSkills = "child/learning-skills"
    const val StudyChat = "child/study-chat"
    const val Activities = "child/activities"
    const val ColorLab = "child/activities/colors"
    const val SentenceBuilder = "child/activities/sentences"
    const val ParentPin = "parent-pin"
    const val Parent = "parent"
    const val AiSettings = "parent/ai-settings"
    const val Settings = "parent/settings"
    const val Progress = "parent/progress"
    const val PracticePicker = "parent/practice"
    const val LearningConcept = "child/learning-concept/{conceptId}"
    const val Books = "books"
    const val AddBook = "books/add"
    const val Book = "books/{bookId}"
    const val Pdf = "books/{bookId}/pdf"
    const val BookSetup = "books/{bookId}/setup"

    fun book(id: String) = "books/$id"
    fun pdf(id: String) = "books/$id/pdf"
    fun setupBook(id: String) = "books/$id/setup"
    fun learningConcept(id: String) = "child/learning-concept/${Uri.encode(id)}"
}

@Composable
private fun MitraNav(
    container: AppContainer,
    onReady: () -> Unit,
) {
    var hasPin by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { hasPin = container.parentPinRepository.hasPin() }
    LaunchedEffect(hasPin) {
        if (hasPin != null) onReady()
    }
    if (hasPin == null) {
        CircularProgressIndicator()
        return
    }

    val nav = rememberNavController()
    val parentUnlocked by container.parentAccessManager.unlocked.collectAsStateWithLifecycle()

    NavHost(navController = nav, startDestination = if (hasPin == true) Routes.Child else Routes.Setup) {
        composable(Routes.Setup) {
            val vm: SetupPinViewModel = viewModel(
                factory = simpleViewModelFactory { SetupPinViewModel(container.parentPinRepository) }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.completed) {
                if (state.completed) {
                    hasPin = true
                    nav.navigate(Routes.Child) { popUpTo(Routes.Setup) { inclusive = true } }
                }
            }
            SetupPinScreen(state, vm::updatePin, vm::updateConfirmation, vm::save)
        }

        composable(Routes.Child) {
            val vm: ChildHomeViewModel = viewModel(
                factory = simpleViewModelFactory {
                    ChildHomeViewModel(container.learningLimitService, container.networkMonitor)
                }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { vm.refresh() }
            ChildHomeScreen(
                state = state,
                onPlay = { if (state.canPlay) nav.navigate(Routes.Learning) },
                onSkills = { if (state.canPlay) nav.navigate(Routes.LearningSkills) },
                onTalk = { if (state.canPlay) nav.navigate(Routes.StudyChat) },
                onActivities = { if (state.canPlay) nav.navigate(Routes.Activities) },
                onBooks = { nav.navigate(Routes.ChildBooks) },
                onParent = { nav.navigate(Routes.ParentPin) },
            )
        }

        composable(Routes.Learning) {
            val vm: LearningSessionViewModel = viewModel(
                factory = simpleViewModelFactory {
                    LearningSessionViewModel(
                        engine = container.learningEngine,
                        speechInput = container.speechInput,
                        speechOutput = container.speechOutput,
                        limitService = container.learningLimitService,
                    )
                }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            LearningSessionScreen(
                state = state,
                onAnswerChange = vm::updateAnswer,
                onSubmit = vm::submit,
                onSelectOption = vm::selectOption,
                onHint = vm::showHint,
                onCompleteParticipation = vm::completeParticipation,
                onSkip = vm::skip,
                onNext = vm::next,
                onStartVoice = vm::startVoiceInput,
                onStopVoice = vm::stopVoiceInput,
                onMicPermissionDenied = vm::onMicrophonePermissionDenied,
                onReplayPrompt = vm::replayPrompt,
                onStop = { vm.stop { nav.popBackStack() } },
                onDone = { nav.popBackStack() },
            )
        }

        composable(Routes.LearningSkills) {
            val vm: LearningSessionViewModel = viewModel(
                key = "standard2-skill-session",
                factory = simpleViewModelFactory {
                    LearningSessionViewModel(
                        engine = container.learningEngine,
                        speechInput = container.speechInput,
                        speechOutput = container.speechOutput,
                        limitService = container.learningLimitService,
                        skillOnly = true,
                    )
                }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            LearningSessionScreen(
                state = state,
                onAnswerChange = vm::updateAnswer,
                onSubmit = vm::submit,
                onSelectOption = vm::selectOption,
                onHint = vm::showHint,
                onCompleteParticipation = vm::completeParticipation,
                onSkip = vm::skip,
                onNext = vm::next,
                onStartVoice = vm::startVoiceInput,
                onStopVoice = vm::stopVoiceInput,
                onMicPermissionDenied = vm::onMicrophonePermissionDenied,
                onReplayPrompt = vm::replayPrompt,
                onStop = { vm.stop { nav.popBackStack() } },
                onDone = { nav.popBackStack() },
            )
        }


        composable(Routes.StudyChat) {
            val vm: StudyChatViewModel = viewModel(
                key = "study-chat",
                factory = simpleViewModelFactory {
                    StudyChatViewModel(
                        contextService = container.studyContextService,
                        aiGateway = container.aiGateway,
                        speechInput = container.speechInput,
                        speechOutput = container.speechOutput,
                        limitService = container.learningLimitService,
                    )
                }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            StudyChatScreen(
                state = state,
                onInput = vm::updateInput,
                onAsk = vm::askCurrent,
                onStartVoice = vm::startVoice,
                onStopVoice = vm::stopVoice,
                onMicDenied = vm::microphoneDenied,
                onHandsFreeChange = vm::setHandsFree,
                onReplay = vm::replayLastAnswer,
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.Activities) {
            ActivityHubScreen(
                onColorLab = { nav.navigate(Routes.ColorLab) },
                onSentenceBuilder = { nav.navigate(Routes.SentenceBuilder) },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.ColorLab) {
            ColorLabScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.SentenceBuilder) {
            SentenceBuilderScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.ChildBooks) {
            val vm: BookListViewModel = viewModel(
                factory = simpleViewModelFactory { BookListViewModel(container.bookRepository) }
            )
            val books by vm.books.collectAsStateWithLifecycle()
            ChildBookListScreen(
                books = books,
                onOpenPdf = { nav.navigate(Routes.pdf(it)) },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.ParentPin) {
            val vm: ParentPinViewModel = viewModel(
                factory = simpleViewModelFactory {
                    ParentPinViewModel(
                        repository = container.parentPinRepository,
                        accessManager = container.parentAccessManager,
                        settingsRepository = container.learningSettingsRepository,
                    )
                }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.unlocked) {
                if (state.unlocked) {
                    nav.navigate(Routes.Parent) { popUpTo(Routes.ParentPin) { inclusive = true } }
                }
            }
            ParentPinScreen(state, vm::updatePin, vm::unlock) { nav.popBackStack() }
        }

        composable(Routes.Parent) {
            ParentProtected(parentUnlocked, onLocked = { nav.navigate(Routes.ParentPin) }) {
                ParentHomeScreen(
                    onBooks = { nav.navigate(Routes.Books) },
                    onProgress = { nav.navigate(Routes.Progress) },
                    onSettings = { nav.navigate(Routes.Settings) },
                    onAiSettings = { nav.navigate(Routes.AiSettings) },
                    onPractice = { nav.navigate(Routes.PracticePicker) },
                    onChildMode = {
                        container.parentAccessManager.lock()
                        nav.navigate(Routes.Child) { popUpTo(Routes.Child) { inclusive = true } }
                    },
                )
            }
        }

        composable(Routes.Settings) {
            ParentProtected(parentUnlocked, onLocked = { nav.navigate(Routes.ParentPin) }) {
                val vm: ParentSettingsViewModel = viewModel(
                    factory = simpleViewModelFactory {
                        ParentSettingsViewModel(container.learningSettingsRepository, container.dataResetService, container.speechOutput, container.backupService)
                    }
                )
                val state by vm.state.collectAsStateWithLifecycle()
                LaunchedEffect(state.fullResetCompleted) {
                    if (state.fullResetCompleted) {
                        hasPin = false
                        nav.navigate(Routes.Setup) { popUpTo(Routes.Child) { inclusive = true } }
                    }
                }
                ParentSettingsScreen(
                    state = state,
                    onSessionMinutes = vm::setSessionMinutes,
                    onDailyMinutes = vm::setDailyMinutes,
                    onParentAccessMinutes = vm::setParentAccessMinutes,
                    onVoiceStyle = vm::setVoiceStyle,
                    onPreviewVoice = vm::previewVoice,
                    onSave = vm::save,
                    onExportBackup = vm::exportBackup,
                    onRestoreBackup = vm::restoreBackup,
                    onResetProgress = vm::resetProgress,
                    onResetBookAnalysis = vm::resetBookAnalysis,
                    onResetEverything = vm::resetEverything,
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(Routes.PracticePicker) {
            ParentProtected(parentUnlocked, onLocked = { nav.navigate(Routes.ParentPin) }) {
                val vm: PracticePickerViewModel = viewModel(
                    factory = simpleViewModelFactory { PracticePickerViewModel(container.progressRepository) }
                )
                val state by vm.state.collectAsStateWithLifecycle()
                PracticePickerScreen(
                    state = state,
                    onSelect = { nav.navigate(Routes.learningConcept(it)) },
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(
            Routes.LearningConcept,
            arguments = listOf(navArgument("conceptId") { type = NavType.StringType }),
        ) { entry ->
            val conceptId = Uri.decode(requireNotNull(entry.arguments?.getString("conceptId")))
            val vm: LearningSessionViewModel = viewModel(
                key = "parent-selected-$conceptId",
                factory = simpleViewModelFactory {
                    LearningSessionViewModel(
                        engine = container.learningEngine,
                        speechInput = container.speechInput,
                        speechOutput = container.speechOutput,
                        limitService = container.learningLimitService,
                        requestedConceptId = conceptId,
                    )
                }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            LearningSessionScreen(
                state = state,
                onAnswerChange = vm::updateAnswer,
                onSubmit = vm::submit,
                onSelectOption = vm::selectOption,
                onHint = vm::showHint,
                onCompleteParticipation = vm::completeParticipation,
                onSkip = vm::skip,
                onNext = vm::next,
                onStartVoice = vm::startVoiceInput,
                onStopVoice = vm::stopVoiceInput,
                onMicPermissionDenied = vm::onMicrophonePermissionDenied,
                onReplayPrompt = vm::replayPrompt,
                onStop = { vm.stop { nav.popBackStack() } },
                onDone = { nav.popBackStack() },
            )
        }

        composable(Routes.Progress) {
            ParentProtected(parentUnlocked, onLocked = { nav.navigate(Routes.ParentPin) }) {
                val vm: ProgressViewModel = viewModel(
                    factory = simpleViewModelFactory { ProgressViewModel(container.progressRepository) }
                )
                val state by vm.state.collectAsStateWithLifecycle()
                ProgressScreen(
                    state = state,
                    onRefresh = vm::refresh,
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(Routes.AiSettings) {
            ParentProtected(parentUnlocked, onLocked = { nav.navigate(Routes.ParentPin) }) {
                val vm: AiSettingsViewModel = viewModel(
                    factory = simpleViewModelFactory {
                        AiSettingsViewModel(
                            repository = container.aiSettingsRepository,
                            secretStore = container.secretStore,
                            gateway = container.configurableAiGateway,
                        )
                    }
                )
                val state by vm.state.collectAsStateWithLifecycle()
                AiSettingsScreen(
                    state = state,
                    onRemoteEnabledChange = vm::setRemoteEnabled,
                    onProviderChange = vm::setProvider,
                    onBaseUrlChange = vm::setBaseUrl,
                    onModelChange = vm::setModel,
                    onCloudflareAccountIdChange = vm::setCloudflareAccountId,
                    onCredentialChange = vm::setCredential,
                    onSave = vm::save,
                    onTest = vm::testConnection,
                    onClearCredential = vm::clearCredential,
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(Routes.Books) {
            ParentProtected(parentUnlocked, onLocked = { nav.navigate(Routes.ParentPin) }) {
                val vm: BookListViewModel = viewModel(
                    factory = simpleViewModelFactory { BookListViewModel(container.bookRepository) }
                )
                val books by vm.books.collectAsStateWithLifecycle()
                BookListScreen(
                    books = books,
                    onAdd = { nav.navigate(Routes.AddBook) },
                    onOpen = { nav.navigate(Routes.book(it)) },
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(Routes.AddBook) {
            ParentProtected(parentUnlocked, onLocked = { nav.navigate(Routes.ParentPin) }) {
                val vm: AddBookViewModel = viewModel(
                    factory = simpleViewModelFactory { AddBookViewModel(container.bookRepository) }
                )
                val state by vm.state.collectAsStateWithLifecycle()
                LaunchedEffect(state.importedBookId) {
                    state.importedBookId?.let { id ->
                        nav.navigate(Routes.book(id)) { popUpTo(Routes.AddBook) { inclusive = true } }
                    }
                }
                AddBookScreen(
                    state = state,
                    onPdfSelected = vm::onPdfSelected,
                    onTitleChange = vm::updateTitle,
                    onSubjectChange = vm::updateSubject,
                    onImport = vm::import,
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(
            route = Routes.Book,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { entry ->
            ParentProtected(parentUnlocked, onLocked = { nav.navigate(Routes.ParentPin) }) {
                val bookId = requireNotNull(entry.arguments?.getString("bookId"))
                val vm: BookDetailViewModel = viewModel(
                    key = "book-$bookId",
                    factory = simpleViewModelFactory {
                        BookDetailViewModel(
                            bookId = bookId,
                            repository = container.bookRepository,
                            knowledgeRepository = container.bookKnowledgeRepository,
                            preparationService = container.bookPreparationService,
                            questionBank = container.offlineQuestionBank,
                        )
                    }
                )
                val book by vm.book.collectAsStateWithLifecycle()
                val chapters by vm.chapters.collectAsStateWithLifecycle()
                val preparingChapterId by vm.preparingChapterId.collectAsStateWithLifecycle()
                val conceptsByChapter by vm.conceptsByChapter.collectAsStateWithLifecycle()
                val offlineQuestionCounts by vm.offlineQuestionCounts.collectAsStateWithLifecycle()
                val message by vm.message.collectAsStateWithLifecycle()
                BookDetailScreen(
                    book = book,
                    chapters = chapters,
                    preparingChapterId = preparingChapterId,
                    conceptsByChapter = conceptsByChapter,
                    offlineQuestionCounts = offlineQuestionCounts,
                    message = message,
                    onOpenPdf = { nav.navigate(Routes.pdf(bookId)) },
                    onSetupChapters = { nav.navigate(Routes.setupBook(bookId)) },
                    onPrepareChapter = vm::prepareChapter,
                    onConceptEnabled = vm::setConceptEnabled,
                    onDelete = { vm.delete { nav.popBackStack() } },
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(
            route = Routes.BookSetup,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { entry ->
            ParentProtected(parentUnlocked, onLocked = { nav.navigate(Routes.ParentPin) }) {
                val bookId = requireNotNull(entry.arguments?.getString("bookId"))
                val vm: BookSetupViewModel = viewModel(
                    key = "book-setup-$bookId",
                    factory = simpleViewModelFactory {
                        BookSetupViewModel(
                            bookId = bookId,
                            bookRepository = container.bookRepository,
                            knowledgeRepository = container.bookKnowledgeRepository,
                            renderer = container.pdfRenderer,
                            preparationService = container.bookPreparationService,
                        )
                    }
                )
                val state by vm.state.collectAsStateWithLifecycle()
                BookSetupScreen(
                    state = state,
                    onPreviousPage = vm::previousPage,
                    onNextPage = vm::nextPage,
                    onToggleTocPage = vm::toggleCurrentTocPage,
                    onDetect = vm::detectChapters,
                    onAddChapter = vm::addDraft,
                    onRemoveChapter = vm::removeDraft,
                    onTitleChange = vm::updateTitle,
                    onStartPageChange = vm::updateStartPage,
                    onEndPageChange = vm::updateEndPage,
                    onSave = vm::save,
                    onBack = { nav.popBackStack() },
                )
            }
        }

        // PDF viewing is intentionally shared by child and parent modes, so it is not PIN-gated.
        composable(
            route = Routes.Pdf,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { entry ->
            val bookId = requireNotNull(entry.arguments?.getString("bookId"))
            val vm: PdfViewerViewModel = viewModel(
                key = "pdf-$bookId",
                factory = simpleViewModelFactory {
                    PdfViewerViewModel(bookId, container.bookRepository, container.pdfRenderer)
                }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            PdfViewerScreen(state, vm::previous, vm::next) { nav.popBackStack() }
        }
    }
}

@Composable
private fun ParentProtected(
    unlocked: Boolean,
    onLocked: () -> Unit,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(unlocked) {
        if (!unlocked) onLocked()
    }
    if (unlocked) content() else CircularProgressIndicator()
}
