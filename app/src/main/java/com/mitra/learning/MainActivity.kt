package com.mitra.learning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mitra.learning.core.AppContainer
import com.mitra.learning.ui.books.AddBookScreen
import com.mitra.learning.ui.books.AddBookViewModel
import com.mitra.learning.ui.books.BookDetailScreen
import com.mitra.learning.ui.books.BookDetailViewModel
import com.mitra.learning.ui.books.BookListScreen
import com.mitra.learning.ui.books.BookListViewModel
import com.mitra.learning.ui.books.PdfViewerScreen
import com.mitra.learning.ui.books.PdfViewerViewModel
import com.mitra.learning.ui.child.ChildHomeScreen
import com.mitra.learning.ui.learning.LearningSessionScreen
import com.mitra.learning.ui.learning.LearningSessionViewModel
import com.mitra.learning.ui.child.ChildBookListScreen
import com.mitra.learning.ui.common.simpleViewModelFactory
import com.mitra.learning.ui.parent.ParentHomeScreen
import com.mitra.learning.ui.parent.ParentPinScreen
import com.mitra.learning.ui.parent.ParentPinViewModel
import com.mitra.learning.ui.setup.SetupPinScreen
import com.mitra.learning.ui.setup.SetupPinViewModel
import com.mitra.learning.ui.theme.MitraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = LocalContext.current.applicationContext as MitraApplication
            MitraTheme { MitraNav(app.container) }
        }
    }
}

private object Routes {
    const val Setup = "setup"
    const val Child = "child"
    const val ChildBooks = "child/books"
    const val Learning = "child/learning"
    const val ParentPin = "parent-pin"
    const val Parent = "parent"
    const val Books = "books"
    const val AddBook = "books/add"
    const val Book = "books/{bookId}"
    const val Pdf = "books/{bookId}/pdf"

    fun book(id: String) = "books/$id"
    fun pdf(id: String) = "books/$id/pdf"
}

@Composable
private fun MitraNav(container: AppContainer) {
    var hasPin by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { hasPin = container.parentPinRepository.hasPin() }
    if (hasPin == null) {
        CircularProgressIndicator()
        return
    }

    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = if (hasPin == true) Routes.Child else Routes.Setup) {
        composable(Routes.Setup) {
            val vm: SetupPinViewModel = viewModel(
                factory = simpleViewModelFactory { SetupPinViewModel(container.parentPinRepository) }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.completed) {
                if (state.completed) {
                    nav.navigate(Routes.Child) { popUpTo(Routes.Setup) { inclusive = true } }
                }
            }
            SetupPinScreen(state, vm::updatePin, vm::updateConfirmation, vm::save)
        }
        composable(Routes.Child) {
            ChildHomeScreen(
                onPlay = { nav.navigate(Routes.Learning) },
                onBooks = { nav.navigate(Routes.ChildBooks) },
                onParent = { nav.navigate(Routes.ParentPin) },
            )
        }
        composable(Routes.Learning) {
            val vm: LearningSessionViewModel = viewModel(
                factory = simpleViewModelFactory { LearningSessionViewModel(container.learningEngine) }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            LearningSessionScreen(
                state = state,
                onAnswerChange = vm::updateAnswer,
                onSubmit = vm::submit,
                onSkip = vm::skip,
                onNext = vm::next,
                onStop = { vm.stop { nav.popBackStack() } },
                onDone = { nav.popBackStack() },
            )
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
                factory = simpleViewModelFactory { ParentPinViewModel(container.parentPinRepository) }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.unlocked) {
                if (state.unlocked) nav.navigate(Routes.Parent) { popUpTo(Routes.ParentPin) { inclusive = true } }
            }
            ParentPinScreen(state, vm::updatePin, vm::unlock) { nav.popBackStack() }
        }
        composable(Routes.Parent) {
            ParentHomeScreen(
                onBooks = { nav.navigate(Routes.Books) },
                onChildMode = { nav.navigate(Routes.Child) { popUpTo(Routes.Child) { inclusive = true } } },
            )
        }
        composable(Routes.Books) {
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
        composable(Routes.AddBook) {
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
        composable(
            route = Routes.Book,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { entry ->
            val bookId = requireNotNull(entry.arguments?.getString("bookId"))
            val vm: BookDetailViewModel = viewModel(
                key = "book-$bookId",
                factory = simpleViewModelFactory { BookDetailViewModel(bookId, container.bookRepository) }
            )
            val book by vm.book.collectAsStateWithLifecycle()
            BookDetailScreen(
                book = book,
                onOpenPdf = { nav.navigate(Routes.pdf(bookId)) },
                onDelete = { vm.delete { nav.popBackStack() } },
                onBack = { nav.popBackStack() },
            )
        }
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
