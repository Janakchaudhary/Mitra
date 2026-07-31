package com.mitra.learning.ui.study

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun StudyChatScreen(
    state: StudyChatUiState,
    onInput: (String) -> Unit,
    onAsk: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onMicDenied: () -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .22f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column(Modifier.weight(1f)) {
                Text("🦁 મિત્ર સાથે વાત કરો", style = MaterialTheme.typography.titleLarge)
                Text("જવાબ તમારા તૈયાર કરેલા પુસ્તકોમાંથી", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onReplay) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Replay answer")
            }
        }

        if (!state.preparedBooksAvailable) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    "Parent mode માં પહેલા એક પુસ્તકનો પાઠ Prepare કરો. પછી મિત્ર એ પુસ્તકમાંથી સવાલોના જવાબ આપશે.",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    StudyWelcomeCard()
                }
            }
            items(state.messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
            if (state.loading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("  પુસ્તકમાં શોધું છું…")
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }

        state.partialTranscript.takeIf { it.isNotBlank() }?.let {
            Text("સાંભળ્યું: “$it”", modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        }

        StudyComposer(
            state = state,
            onInput = onInput,
            onAsk = onAsk,
            onStartVoice = onStartVoice,
            onStopVoice = onStopVoice,
            onMicDenied = onMicDenied,
        )
    }
}

@Composable
private fun StudyWelcomeCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📚 કંઈ પણ પૂછો!", style = MaterialTheme.typography.headlineSmall)
            Text("ઉદાહરણ: ‘આ પાઠમાં હાથી વિશે શું લખ્યું છે?’")
            Text("અથવા: ‘૨૭ + ૧૮ કેવી રીતે કરીએ?’")
            Text("મિત્ર માત્ર તૈયાર કરેલા textbook context પરથી જવાબ આપશે.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MessageBubble(message: StudyMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.speaker == StudySpeaker.CHILD) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.88f),
            shape = RoundedCornerShape(22.dp),
            color = if (message.speaker == StudySpeaker.CHILD)
                MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.primaryContainer,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (message.speaker == StudySpeaker.CHILD) "તમે" else "🦁 મિત્ર", style = MaterialTheme.typography.labelLarge)
                Text(message.text, style = MaterialTheme.typography.bodyLarge)
                if (message.sources.isNotEmpty()) {
                    Text("📖 ${message.sources.joinToString(" • ")}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StudyComposer(
    state: StudyChatUiState,
    onInput: (String) -> Unit,
    onAsk: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onMicDenied: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) onStartVoice() else onMicDenied()
    }

    Surface(tonalElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInput,
                placeholder = { Text("મારો સવાલ…") },
                modifier = Modifier.weight(1f),
                enabled = !state.loading,
                maxLines = 3,
            )
            IconButton(
                enabled = state.speechAvailable && !state.loading,
                onClick = {
                    if (!granted) launcher.launch(Manifest.permission.RECORD_AUDIO)
                    else if (state.listening) onStopVoice() else onStartVoice()
                },
            ) {
                Icon(
                    if (state.listening) Icons.Default.StopCircle else Icons.Default.Mic,
                    contentDescription = "Voice question",
                    tint = if (state.listening) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
            Button(onClick = onAsk, enabled = state.input.isNotBlank() && !state.loading) {
                Icon(Icons.Default.Send, contentDescription = null)
            }
        }
    }
}
