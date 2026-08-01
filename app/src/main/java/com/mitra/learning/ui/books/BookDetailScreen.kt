package com.mitra.learning.ui.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mitra.learning.data.db.entity.BookEntity
import com.mitra.learning.data.db.entity.ChapterAnalysisStatus
import com.mitra.learning.data.db.entity.ChapterEntity
import com.mitra.learning.data.db.entity.ConceptEntity

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: BookEntity?,
    chapters: List<ChapterEntity>,
    preparingChapterId: String?,
    conceptsByChapter: Map<String, List<ConceptEntity>>,
    offlineQuestionCounts: Map<String, Int>,
    message: String?,
    onOpenPdf: () -> Unit,
    onSetupChapters: () -> Unit,
    onPrepareChapter: (String) -> Unit,
    onConceptEnabled: (String, String, Boolean) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book?.title ?: "Book") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                if (book == null) {
                    Text("Loading…")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(book.title, style = MaterialTheme.typography.headlineMedium)
                        Text("Subject: ${book.subject}")
                        Text("Standard: ${book.standard}")
                        Text("Medium: ${book.language}")
                        Text("Pages: ${book.pageCount}")
                        Text("Preparation: ${book.analysisStatus.name.replace('_', ' ')}")
                        Button(onClick = onOpenPdf, modifier = Modifier.fillMaxWidth()) { Text("Open PDF") }
                        Button(onClick = onSetupChapters, modifier = Modifier.fillMaxWidth()) {
                            Text(if (chapters.isEmpty()) "Set up chapters" else "Review / edit chapters")
                        }
                        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            if (chapters.isNotEmpty()) {
                item {
                    HorizontalDivider()
                    Text("Chapters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                    Text("Prepare one chapter at a time. Prepared data is cached locally.")
                }
                items(chapters, key = { it.id }) { chapter ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${chapter.chapterNumber ?: ""}. ${chapter.titleGujarati}", style = MaterialTheme.typography.titleMedium)
                                Text("Pages ${chapter.startPage}–${chapter.endPage}")
                                Text(chapter.analysisStatus.name.replace('_', ' '), style = MaterialTheme.typography.bodySmall)
                                val detected = conceptsByChapter[chapter.id].orEmpty()
                                if (detected.isNotEmpty()) {
                                    Text("Detected learning goals:", style = MaterialTheme.typography.labelMedium)
                                    detected.take(4).forEach { concept ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(concept.titleGujarati, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    "Offline: ${offlineQuestionCounts[concept.id] ?: 0}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                                Switch(
                                                    checked = concept.practiceReady,
                                                    onCheckedChange = { onConceptEnabled(chapter.id, concept.id, it) },
                                                )
                                            }
                                        }
                                    }
                                    if (detected.size > 4) Text("+ ${detected.size - 4} more", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Button(
                                onClick = { onPrepareChapter(chapter.id) },
                                enabled = preparingChapterId == null && chapter.analysisStatus != ChapterAnalysisStatus.PREPARING,
                            ) {
                                Text(if (preparingChapterId == chapter.id) "Preparing…" else if (chapter.analysisStatus == ChapterAnalysisStatus.READY) "Prepare again" else "Prepare")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
            item {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Text("Remove book")
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove book?") },
            text = { Text("The private PDF copy, chapter analysis, and book-derived concepts will be removed from Mitra.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
