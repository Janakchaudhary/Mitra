package com.mitra.learning.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mitra.learning.voice.VoiceStyle

@Composable
fun ParentSettingsScreen(
    state: ParentSettingsUiState,
    onSessionMinutes: (Int) -> Unit,
    onDailyMinutes: (Int) -> Unit,
    onParentAccessMinutes: (Int) -> Unit,
    onVoiceStyle: (VoiceStyle) -> Unit,
    onPreviewVoice: () -> Unit,
    onSave: () -> Unit,
    onResetProgress: () -> Unit,
    onResetBookAnalysis: () -> Unit,
    onResetEverything: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmAction by remember { mutableStateOf<ResetAction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Parent settings", style = MaterialTheme.typography.headlineMedium)
        }

        if (state.loading) {
            CircularProgressIndicator()
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Learning limits", style = MaterialTheme.typography.titleLarge)
                    Text("Session length")
                    OptionGrid(listOf(15, 20, 30), state.sessionMinutes, onSessionMinutes, suffix = "min")
                    Text("Daily learning allowance")
                    OptionGrid(listOf(15, 30, 45, 60), state.dailyMinutes, onDailyMinutes, suffix = "min")
                    Text("Parent access relock")
                    OptionGrid(listOf(2, 5, 10, 15), state.parentAccessMinutes, onParentAccessMinutes, suffix = "min")
                    Text("Mitra voice", style = MaterialTheme.typography.titleMedium)
                    VoiceStyle.entries.forEach { style ->
                        if (style == state.voiceStyle) {
                            Button(onClick = { onVoiceStyle(style) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(style.label)
                            }
                        } else {
                            OutlinedButton(onClick = { onVoiceStyle(style) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(style.label)
                            }
                        }
                        Text(style.description, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = onPreviewVoice,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Preview voice") }
                    Button(
                        onClick = onSave,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Text("Save settings")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Privacy & data", style = MaterialTheme.typography.titleLarge)
                    Text("Books, progress and prepared lesson data are stored locally on this phone.")
                    Text("Raw microphone audio is not saved by Mitra.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { confirmAction = ResetAction.PROGRESS },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) { Text("Reset learning progress") }
                    OutlinedButton(
                        onClick = { confirmAction = ResetAction.ANALYSIS },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) { Text("Remove prepared book analysis") }
                    OutlinedButton(
                        onClick = { confirmAction = ResetAction.EVERYTHING },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) { Text("Reset entire app") }
                }
            }

            state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (state.busy) CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
        }
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(action.title) },
            text = { Text(action.description) },
            confirmButton = {
                TextButton(onClick = {
                    confirmAction = null
                    when (action) {
                        ResetAction.PROGRESS -> onResetProgress()
                        ResetAction.ANALYSIS -> onResetBookAnalysis()
                        ResetAction.EVERYTHING -> onResetEverything()
                    }
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun OptionGrid(
    options: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    suffix: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { value ->
                    val modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    if (value == selected) {
                        Button(onClick = { onSelected(value) }, modifier = modifier) { Text("$value $suffix") }
                    } else {
                        OutlinedButton(onClick = { onSelected(value) }, modifier = modifier) { Text("$value $suffix") }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private enum class ResetAction(val title: String, val description: String) {
    PROGRESS("Reset learning progress?", "Mastery, attempts and session history will be removed. Books remain."),
    ANALYSIS("Remove prepared book analysis?", "Prepared page knowledge and book-derived concepts will be removed. PDFs and chapter ranges remain."),
    EVERYTHING("Reset entire app?", "Books, progress, settings, AI credential and parent PIN will be removed from this phone."),
}
