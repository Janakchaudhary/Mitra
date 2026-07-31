package com.mitra.learning.ui.learning

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun LearningSessionScreen(
    state: LearningSessionUiState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onMicPermissionDenied: () -> Unit,
    onReplayPrompt: () -> Unit,
    onStop: () -> Unit,
    onDone: () -> Unit,
) {
    LaunchedEffect(state.exitRequested) {
        if (state.exitRequested) onStop()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            state.completed -> CompletedContent(state, onDone)
            state.loading && state.sessionId == null -> LoadingContent()
            else -> SessionContent(
                state = state,
                onAnswerChange = onAnswerChange,
                onSubmit = onSubmit,
                onSkip = onSkip,
                onNext = onNext,
                onStartVoice = onStartVoice,
                onStopVoice = onStopVoice,
                onMicPermissionDenied = onMicPermissionDenied,
                onReplayPrompt = onReplayPrompt,
                onStop = onStop,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("રમત તૈયાર થઈ રહી છે…")
    }
}

@Composable
private fun SessionContent(
    state: LearningSessionUiState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onMicPermissionDenied: () -> Unit,
    onReplayPrompt: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("🦁 મિત્ર", style = MaterialTheme.typography.headlineMedium)
        if (state.conceptTitleGujarati.isNotBlank()) {
            Text(state.conceptTitleGujarati, style = MaterialTheme.typography.titleMedium)
        }

        val current = state.currentQuestion
        if (current != null) {
            Text(
                "પ્રશ્ન ${state.questionIndex + 1} / ${state.questions.size}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(current.promptGujarati, style = MaterialTheme.typography.headlineSmall)

            OutlinedButton(
                onClick = onReplayPrompt,
                enabled = !state.listening && state.ttsAvailable != false,
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null)
                Text(if (state.ttsSpeaking) "  બોલું છું…" else "  ફરી સાંભળો")
            }

            VoiceAnswerControl(
                state = state,
                onStartVoice = onStartVoice,
                onStopVoice = onStopVoice,
                onMicPermissionDenied = onMicPermissionDenied,
            )

            OutlinedTextField(
                value = state.answer,
                onValueChange = onAnswerChange,
                label = { Text("અથવા જવાબ લખો") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !state.awaitingNext && !state.loading && !state.listening,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            state.feedback?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
            state.voiceMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.ttsAvailable == false) {
                Text(
                    "આ ફોનમાં ગુજરાતી બોલવાનો અવાજ ઉપલબ્ધ નથી; લખાણ ચાલુ રહેશે.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (state.awaitingNext) {
                Button(
                    onClick = onNext,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.questionIndex == state.questions.lastIndex) "પૂર્ણ કરીએ" else "આગળ")
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = !state.loading && !state.listening,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("જવાબ આપો")
                }
                OutlinedButton(
                    onClick = onSkip,
                    enabled = !state.loading && !state.listening,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("આ પ્રશ્ન છોડો")
                }
            }
        } else {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = onStop, enabled = !state.loading) {
                Text("■ બસ")
            }
        }
    }
}

@Composable
private fun VoiceAnswerControl(
    state: LearningSessionUiState,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onMicPermissionDenied: () -> Unit,
) {
    val context = LocalContext.current
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (granted) onStartVoice() else onMicPermissionDenied()
    }

    val enabled = state.speechInputAvailable && !state.loading && !state.awaitingNext
    val gestureModifier = Modifier
        .fillMaxWidth()
        .semantics {
            role = Role.Button
            contentDescription = if (state.listening) "માઇક્રોફોન સાંભળી રહ્યો છે" else "જવાબ બોલવા માઇક્રોફોન"
            onClick {
                if (!enabled) {
                    false
                } else {
                    if (!micGranted) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    else if (state.listening) onStopVoice()
                    else onStartVoice()
                    true
                }
            }
        }
        .pointerInput(enabled, micGranted) {
            detectTapGestures(
                onPress = {
                    if (enabled) {
                        if (!micGranted) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            onStartVoice()
                            tryAwaitRelease()
                            onStopVoice()
                        }
                    }
                }
            )
        }

    Surface(
        modifier = gestureModifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (state.listening) 6.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (state.listening) Icons.Default.StopCircle else Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
            )
            Text(
                when {
                    !state.speechInputAvailable -> "આ ફોનમાં voice input ઉપલબ્ધ નથી"
                    state.listening -> "બોલો… છોડશો ત્યારે જવાબ લઉં"
                    else -> "દબાવી રાખીને જવાબ બોલો"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.partialTranscript.isNotBlank()) {
                Text("“${state.partialTranscript}”", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun CompletedContent(
    state: LearningSessionUiState,
    onDone: () -> Unit,
) {
    val summary = requireNotNull(state.summary)
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🌟", style = MaterialTheme.typography.displayLarge)
        Text("આજની રમત પૂરી!", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(summary.conceptTitleGujarati, style = MaterialTheme.typography.titleLarge)
        Text("${summary.correct} / ${summary.attempts} જવાબ સાચા")
        Spacer(Modifier.height(24.dp))
        Text("હવે ફોનને થોડો આરામ આપીએ. 😊")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("ઘરે પાછા")
        }
    }
}
