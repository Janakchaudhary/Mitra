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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
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
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.EvaluationMode

@Composable
fun LearningSessionScreen(
    state: LearningSessionUiState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSelectOption: (String) -> Unit,
    onHint: () -> Unit,
    onCompleteParticipation: () -> Unit,
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
                onSelectOption = onSelectOption,
                onHint = onHint,
                onCompleteParticipation = onCompleteParticipation,
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
    onSelectOption: (String) -> Unit,
    onHint: () -> Unit,
    onCompleteParticipation: () -> Unit,
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
        state.remainingSessionSeconds?.let { remaining ->
            val minutes = remaining / 60
            val seconds = remaining % 60
            Text(
                "સમય બાકી %02d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        val current = state.currentQuestion
        if (current != null) {
            Text(
                "પ્રવૃત્તિ ${state.questionIndex + 1} / ${state.questions.size} • ${activityLabel(current.type)}",
                style = MaterialTheme.typography.labelLarge,
            )
            current.sourcePage?.let {
                Text("📖 પુસ્તક પાનું $it", style = MaterialTheme.typography.bodySmall)
            }
            Text(current.promptGujarati, style = MaterialTheme.typography.headlineSmall)

            OutlinedButton(
                onClick = onReplayPrompt,
                enabled = !state.listening && state.ttsAvailable != false,
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                Text(if (state.ttsSpeaking) "  બોલું છું…" else "  ફરી સાંભળો")
            }

            val showVoice = current.type != ActivityType.SPELLING && (
                current.evaluationMode != EvaluationMode.PARTICIPATION ||
                    current.type in setOf(ActivityType.TEACH_MITRA, ActivityType.STORY, ActivityType.RECAP)
                )
            if (showVoice) {
                VoiceAnswerControl(
                    state = state,
                    onStartVoice = onStartVoice,
                    onStopVoice = onStopVoice,
                    onMicPermissionDenied = onMicPermissionDenied,
                )
            }

            when (current.evaluationMode) {
                EvaluationMode.MULTIPLE_CHOICE -> {
                    current.optionsGujarati.forEach { option ->
                        OutlinedButton(
                            onClick = { onSelectOption(option) },
                            enabled = !state.awaitingNext && !state.loading && !state.listening,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(option)
                        }
                    }
                }
                EvaluationMode.PARTICIPATION -> {
                    if (current.type == ActivityType.TEACH_MITRA && state.answer.isNotBlank()) {
                        Text("તમે સમજાવ્યું: “${state.answer}”", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = state.answer,
                        onValueChange = onAnswerChange,
                        label = { Text(if (current.type == ActivityType.SPELLING) "શબ્દ લખો" else if (current.evaluationMode == EvaluationMode.NUMERIC) "અથવા જવાબ લખો" else "અથવા ટૂંકો જવાબ લખો") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (current.evaluationMode == EvaluationMode.NUMERIC) KeyboardType.Number else KeyboardType.Text
                        ),
                        enabled = !state.awaitingNext && !state.loading && !state.listening,
                        singleLine = current.evaluationMode == EvaluationMode.NUMERIC || current.type in setOf(ActivityType.SPELLING, ActivityType.MISSING_LETTER, ActivityType.VOCABULARY),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            current.hintGujarati?.takeIf { it.isNotBlank() }?.let {
                if (!state.awaitingNext) {
                    OutlinedButton(onClick = onHint, enabled = !state.loading && !state.listening) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null)
                        Text("  સંકેત")
                    }
                }
            }
            state.hintText?.let {
                Text("💡 $it", style = MaterialTheme.typography.bodyLarge)
            }

            state.feedback?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            state.voiceMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (state.ttsAvailable == false) {
                Text("આ ફોનમાં ગુજરાતી બોલવાનો અવાજ ઉપલબ્ધ નથી; લખાણ ચાલુ રહેશે.", style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (state.awaitingNext) {
                Button(onClick = onNext, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.questionIndex == state.questions.lastIndex) "પૂર્ણ કરીએ" else "આગળ")
                }
            } else {
                when (current.evaluationMode) {
                    EvaluationMode.PARTICIPATION -> Button(
                        onClick = onCompleteParticipation,
                        enabled = !state.loading && !state.listening,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(current.completionButtonGujarati) }
                    EvaluationMode.MULTIPLE_CHOICE -> Unit
                    else -> Button(
                        onClick = onSubmit,
                        enabled = !state.loading && !state.listening,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("જવાબ આપો") }
                }
                OutlinedButton(
                    onClick = onSkip,
                    enabled = !state.loading && !state.listening,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("આ પ્રવૃત્તિ છોડો") }
            }
        } else {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = onStop, enabled = !state.loading) { Text("■ બસ") }
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
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
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
                if (!enabled) false else {
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
                    state.listening -> "બોલો… છોડશો ત્યારે સાંભળું"
                    else -> "દબાવી રાખીને બોલો"
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
private fun CompletedContent(state: LearningSessionUiState, onDone: () -> Unit) {
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
        Text("${summary.attempts} પ્રવૃત્તિ પૂરી")
        if (summary.assessed > 0) Text("${summary.correct} / ${summary.assessed} ચકાસેલા જવાબ સાચા")
        if (summary.participationActivities > 0) Text("${summary.participationActivities} શોધ/રમત પ્રવૃત્તિ")
        Spacer(Modifier.height(24.dp))
        Text(
            if (state.timeLimitReached) "આજની રમતનો સમય પૂરો થયો. હવે ફોનને થોડો આરામ આપીએ. 😊"
            else "હવે ફોનને થોડો આરામ આપીએ. 😊"
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("ઘરે પાછા") }
    }
}

private fun activityLabel(type: ActivityType): String = when (type) {
    ActivityType.QUESTION -> "પ્રશ્ન"
    ActivityType.MULTIPLE_CHOICE -> "પસંદગી"
    ActivityType.RIDDLE -> "ઉખાણું"
    ActivityType.STORY -> "વાર્તા"
    ActivityType.BOOK_LOOK -> "પુસ્તક શોધ"
    ActivityType.READING -> "વાંચન"
    ActivityType.VOCABULARY -> "શબ્દ રમત"
    ActivityType.SPELLING -> "જોડણી"
    ActivityType.MISSING_LETTER -> "ખૂટતો અક્ષર"
    ActivityType.TABLES -> "પહાડો"
    ActivityType.WORD_PROBLEM -> "વાર્તાનો હિસાબ"
    ActivityType.PHYSICAL_MISSION -> "મિશન"
    ActivityType.DRAW -> "ચિત્ર"
    ActivityType.TEACH_MITRA -> "મિત્રને શીખવો"
    ActivityType.RECAP -> "યાદ કરીએ"
}
