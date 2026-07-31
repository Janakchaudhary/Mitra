package com.mitra.learning.ui.books

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BookSetupScreen(
    state: BookSetupUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleTocPage: () -> Unit,
    onDetect: () -> Unit,
    onAddChapter: () -> Unit,
    onRemoveChapter: (String) -> Unit,
    onTitleChange: (String, String) -> Unit,
    onStartPageChange: (String, String) -> Unit,
    onEndPageChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("1. Find the contents / index page", style = MaterialTheme.typography.titleLarge)
                Text("Use Previous/Next, mark one or more contents pages, then detect chapters. You can also skip detection and add chapters manually.")
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    when {
                        state.loadingPage -> CircularProgressIndicator()
                        state.bitmap != null -> Image(
                            bitmap = state.bitmap.asImageBitmap(),
                            contentDescription = "PDF page ${state.pageIndex + 1}",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = onPreviousPage, enabled = state.pageIndex > 0 && !state.loadingPage) { Text("Previous") }
                        Text("${state.pageIndex + 1} / ${state.pageCount.coerceAtLeast(1)}")
                        OutlinedButton(onClick = onNextPage, enabled = state.pageIndex + 1 < state.pageCount && !state.loadingPage) { Text("Next") }
                    }
                    val marked = state.pageIndex in state.selectedTocPages
                    OutlinedButton(onClick = onToggleTocPage, modifier = Modifier.fillMaxWidth()) {
                        Text(if (marked) "✓ Page ${state.pageIndex + 1} marked as contents" else "Mark page ${state.pageIndex + 1} as contents")
                    }
                    if (state.selectedTocPages.isNotEmpty()) {
                        Text("Selected: ${state.selectedTocPages.sorted().joinToString { (it + 1).toString() }}")
                    }
                    Button(
                        onClick = onDetect,
                        enabled = state.selectedTocPages.isNotEmpty() && !state.detecting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (state.detecting) "Detecting…" else "Detect chapter structure") }
                }
            }
            item { HorizontalDivider() }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("2. Review chapters", style = MaterialTheme.typography.titleLarge)
                        state.sourceLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    IconButton(onClick = onAddChapter) { Icon(Icons.Default.Add, contentDescription = "Add chapter") }
                }
            }
            items(state.drafts, key = { it.id }) { draft ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Chapter ${draft.chapterNumber ?: ""}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemoveChapter(draft.id) }) { Icon(Icons.Default.Delete, contentDescription = "Remove chapter") }
                    }
                    OutlinedTextField(
                        value = draft.titleGujarati,
                        onValueChange = { onTitleChange(draft.id, it) },
                        label = { Text("Chapter title") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = draft.startPage.toString(),
                            onValueChange = { onStartPageChange(draft.id, it) },
                            label = { Text("Start page") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = draft.endPage.toString(),
                            onValueChange = { onEndPageChange(draft.id, it) },
                            label = { Text("End page") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    HorizontalDivider()
                }
            }
            item {
                OutlinedButton(onClick = onAddChapter, modifier = Modifier.fillMaxWidth()) { Text("+ Add chapter manually") }
                Button(onClick = onSave, enabled = state.drafts.isNotEmpty() && !state.saving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.saving) "Saving…" else "Save chapter structure")
                }
                state.message?.let { Text(it) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
