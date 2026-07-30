package com.mitra.learning.ui.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mitra.learning.data.db.entity.BookEntity

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: BookEntity?,
    onOpenPdf: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book?.title ?: "Book") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (book == null) {
                Text("Loading…")
            } else {
                Text(book.title, style = MaterialTheme.typography.headlineMedium)
                Text("Subject: ${book.subject}")
                Text("Standard: ${book.standard}")
                Text("Medium: ${book.language}")
                Text("Pages: ${book.pageCount}")
                Text("Preparation: ${book.analysisStatus.name.replace('_', ' ')}")
                Button(onClick = onOpenPdf, modifier = Modifier.fillMaxWidth()) { Text("Open PDF") }
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Remove book")
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove book?") },
            text = { Text("The private copy of this PDF will be deleted from Mitra.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
