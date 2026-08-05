package com.mitra.learning.ui.books

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mitra.learning.data.db.entity.BookEntity

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    books: List<BookEntity>,
    importState: PreparedBookImportUiState,
    onAdd: () -> Unit,
    onImportPrepared: (Uri) -> Unit,
    onCopyChatGptPrompt: () -> Unit,
    onOpenChatGpt: () -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val preparedBookLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportPrepared) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My books") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add PDF book")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Text(
                                "  Prepare with ChatGPT",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        Text(
                            "Upload the textbook to ChatGPT, use Mitra's preparation prompt, download the .mitrabook JSON file, then import it here. The book, meanings and tests work offline after import.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onCopyChatGptPrompt,
                                enabled = !importState.importing,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Text("  Copy prompt")
                            }
                            OutlinedButton(
                                onClick = onOpenChatGpt,
                                enabled = !importState.importing,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Text("  Open ChatGPT")
                            }
                        }
                        Button(
                            onClick = {
                                preparedBookLauncher.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/json",
                                        "application/zip",
                                        "application/octet-stream",
                                        "text/plain",
                                    )
                                )
                            },
                            enabled = !importState.importing,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) {
                            if (importState.importing) {
                                CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp))
                                Text("Importing…")
                            } else {
                                Text("Import prepared book (.mitrabook / .json)")
                            }
                        }
                        importState.message?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (books.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                            Text("No books yet")
                            Text("Tap + to add a PDF, or import a ChatGPT-prepared book")
                        }
                    }
                }
            } else {
                items(books, key = { it.id }) { book ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(book.id) }
                            .padding(vertical = 14.dp),
                    ) {
                        Column {
                            Text(book.title, style = MaterialTheme.typography.titleMedium)
                            Text("${book.subject} • Std ${book.standard} • ${book.pageCount} pages")
                            if (book.localPdfPath.isBlank()) {
                                Text(
                                    "ChatGPT-prepared package • ready offline",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
