package com.mitra.learning.ui.learning

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mitra.learning.learning.model.ActivityType
import com.mitra.learning.learning.model.ArithmeticWork
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
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
        Text("🦁", style = MaterialTheme.typography.displayLarge)
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("તમારી નવી રમત તૈયાર થાય છે…", style = MaterialTheme.typography.titleMedium)
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
    val current = state.currentQuestion
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("🦁", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.headlineMedium)
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text("મિત્ર સાથે શીખીએ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (state.conceptTitleGujarati.isNotBlank()) {
                        Text(state.conceptTitleGujarati, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            state.remainingSessionSeconds?.let { remaining ->
                AssistChip(
                    onClick = {},
                    label = { Text("⏱ %02d:%02d".format(remaining / 60, remaining % 60)) },
                )
            }
        }

        if (current != null) {
            val progress = (state.questionIndex + 1).toFloat() / state.questions.size.coerceAtLeast(1).toFloat()
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${activityEmoji(current.type)} ${activityLabel(current.type)}  •  ${state.questionIndex + 1}/${state.questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    current.sourcePage?.let { Text("📖 પુસ્તક પાનું $it", style = MaterialTheme.typography.labelLarge) }
                    Text(current.promptGujarati, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onReplayPrompt, enabled = !state.listening && state.ttsAvailable != false) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Text(if (state.ttsSpeaking) "  બોલું છું…" else "  ફરી સાંભળો")
                }
                current.hintGujarati?.takeIf { it.isNotBlank() }?.let {
                    OutlinedButton(onClick = onHint, enabled = !state.awaitingNext && !state.loading && !state.listening) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null)
                        Text("  સંકેત")
                    }
                }
            }

            current.arithmeticWork?.let { work ->
                RoughWorkBoard(questionId = current.id, work = work, enabled = !state.awaitingNext && !state.loading)
            }

            val showVoice = current.type != ActivityType.SPELLING && (
                current.evaluationMode != EvaluationMode.PARTICIPATION ||
                    current.type in setOf(ActivityType.TEACH_MITRA, ActivityType.STORY, ActivityType.RECAP)
                )
            if (showVoice) {
                VoiceAnswerControl(state, onStartVoice, onStopVoice, onMicPermissionDenied)
            }

            when (current.evaluationMode) {
                EvaluationMode.MULTIPLE_CHOICE -> current.optionsGujarati.forEach { option ->
                    FilledTonalButton(
                        onClick = { onSelectOption(option) },
                        enabled = !state.awaitingNext && !state.loading && !state.listening,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text(option, style = MaterialTheme.typography.titleMedium) }
                }
                EvaluationMode.PARTICIPATION -> if (current.type == ActivityType.TEACH_MITRA && state.answer.isNotBlank()) {
                    Text("તમે સમજાવ્યું: “${state.answer}”", style = MaterialTheme.typography.bodyLarge)
                }
                else -> OutlinedTextField(
                    value = state.answer,
                    onValueChange = onAnswerChange,
                    label = {
                        Text(
                            if (current.type == ActivityType.SPELLING) "શબ્દ લખો"
                            else if (current.evaluationMode == EvaluationMode.NUMERIC) "અંતિમ જવાબ લખો"
                            else "ટૂંકો જવાબ લખો"
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (current.evaluationMode == EvaluationMode.NUMERIC) KeyboardType.Number else KeyboardType.Text
                    ),
                    enabled = !state.awaitingNext && !state.loading && !state.listening,
                    singleLine = current.evaluationMode == EvaluationMode.NUMERIC || current.type in setOf(
                        ActivityType.SPELLING, ActivityType.MISSING_LETTER, ActivityType.VOCABULARY
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                )
            }

            state.hintText?.let {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text("💡 $it", modifier = Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
            state.feedback?.let {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(it, modifier = Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
            state.voiceMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (state.ttsAvailable == false) {
                Text("આ ફોનમાં ગુજરાતી અવાજ ઉપલબ્ધ નથી; લખાણ ચાલુ રહેશે.", style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (state.awaitingNext) {
                Button(onClick = onNext, enabled = !state.loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Text(if (state.questionIndex == state.questions.lastIndex) "પૂર્ણ કરીએ" else "આગળનો નવો પ્રશ્ન")
                }
            } else {
                when (current.evaluationMode) {
                    EvaluationMode.PARTICIPATION -> Button(
                        onClick = onCompleteParticipation,
                        enabled = !state.loading && !state.listening,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) { Text(current.completionButtonGujarati) }
                    EvaluationMode.MULTIPLE_CHOICE -> Unit
                    else -> Button(
                        onClick = onSubmit,
                        enabled = !state.loading && !state.listening,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) { Text("જવાબ ચકાસો") }
                }
                OutlinedButton(
                    onClick = onSkip,
                    enabled = !state.loading && !state.listening,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("આ પ્રશ્ન છોડો") }
            }
        } else {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        OutlinedButton(onClick = onStop, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text("■ આજ માટે બસ")
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun RoughWorkBoard(questionId: String, work: ArithmeticWork, enabled: Boolean) {
    val strokes = remember(questionId) { mutableStateListOf<List<Offset>>() }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val inkColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("✍️ રફ કામ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (work.regrouping) "દશક અને એકમ ગોઠવો; કેરિ/ઉધાર લખી શકો."
                        else "આંગળીથી લખીને પગલાં ગણો.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }, enabled = enabled && strokes.isNotEmpty()) {
                        Icon(Icons.Default.Undo, contentDescription = "છેલ્લી લીટી દૂર કરો")
                    }
                    OutlinedButton(onClick = { strokes.clear() }, enabled = enabled && strokes.isNotEmpty()) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "રફ કામ સાફ કરો")
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("દશક   એકમ", style = MaterialTheme.typography.labelMedium)
                    Text(work.top.toString(), style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace)
                    Text("${work.operator} ${work.bottom}", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace)
                    Text("────", style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(questionId, enabled) {
                            if (!enabled) return@pointerInput
                            detectDragGestures(
                                onDragStart = { offset -> strokes.add(listOf(offset)) },
                                onDrag = { change, _ ->
                                    if (strokes.isNotEmpty()) {
                                        strokes[strokes.lastIndex] = strokes.last() + change.position
                                    }
                                },
                            )
                        }
                ) {
                    val grid = 32.dp.toPx()
                    var x = grid
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                        x += grid
                    }
                    var y = grid
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        y += grid
                    }
                    strokes.forEach { points ->
                        if (points.size == 1) {
                            drawCircle(inkColor, radius = 2.5.dp.toPx(), center = points.first())
                        } else if (points.size > 1) {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                points.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(path, inkColor, style = Stroke(width = 4.dp.toPx()))
                        }
                    }
                }
                if (strokes.isEmpty()) {
                    Text(
                        "અહીં આંગળીથી લખો…",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
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
                    else if (state.listening) onStopVoice() else onStartVoice()
                    true
                }
            }
        }
        .pointerInput(enabled, micGranted) {
            detectTapGestures(onPress = {
                if (enabled) {
                    if (!micGranted) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    else {
                        onStartVoice()
                        tryAwaitRelease()
                        onStopVoice()
                    }
                }
            })
        }

    Surface(modifier = gestureModifier, shape = RoundedCornerShape(20.dp), tonalElevation = if (state.listening) 6.dp else 1.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(if (state.listening) Icons.Default.StopCircle else Icons.Default.Mic, null, Modifier.size(38.dp))
            Text(
                when {
                    !state.speechInputAvailable -> "આ ફોનમાં voice input ઉપલબ્ધ નથી"
                    state.listening -> "બોલો… છોડશો ત્યારે જવાબ ચકાસીશ"
                    else -> "દબાવી રાખીને જવાબ બોલો"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.partialTranscript.isNotBlank()) Text("“${state.partialTranscript}”", style = MaterialTheme.typography.bodyLarge)
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
        Text(if (state.timeLimitReached) "આજનો સમય પૂરો થયો. હવે ફોનને આરામ આપીએ. 😊" else "હવે ફોનને થોડો આરામ આપીએ. 😊")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("ઘરે પાછા") }
    }
}

private fun activityLabel(type: ActivityType): String = when (type) {
    ActivityType.QUESTION -> "ગણિત ચેલેન્જ"
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

private fun activityEmoji(type: ActivityType): String = when (type) {
    ActivityType.QUESTION -> "🧮"
    ActivityType.MULTIPLE_CHOICE -> "🎯"
    ActivityType.RIDDLE -> "🕵️"
    ActivityType.STORY -> "📚"
    ActivityType.BOOK_LOOK -> "🔎"
    ActivityType.READING -> "📖"
    ActivityType.VOCABULARY -> "🗣️"
    ActivityType.SPELLING -> "✏️"
    ActivityType.MISSING_LETTER -> "🔤"
    ActivityType.TABLES -> "✖️"
    ActivityType.WORD_PROBLEM -> "🍎"
    ActivityType.PHYSICAL_MISSION -> "🏃"
    ActivityType.DRAW -> "🎨"
    ActivityType.TEACH_MITRA -> "🦁"
    ActivityType.RECAP -> "⭐"
}
