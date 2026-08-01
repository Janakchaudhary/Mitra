package com.mitra.learning.ui.learning

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.mitra.learning.learning.evaluation.GuidedMathCoach
import com.mitra.learning.learning.evaluation.GujaratiNumberNormalizer
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.AnimatedScale
import com.mitra.learning.ui.animation.MascotMood
import com.mitra.learning.ui.animation.SuccessBurst
import com.mitra.learning.ui.animation.ThinkingDots

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

    val successFeedback = state.feedback?.takeIf { feedback ->
        listOf("સાચું", "Perfect", "બહુ સરસ", "શાબાશ").any { feedback.contains(it, ignoreCase = true) }
    }
    AnimatedLearningBackground(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.82f),
        ) {
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
        SuccessBurst(trigger = successFeedback, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedMitraMascot(mood = MascotMood.THINKING, size = 112.dp)
        ThinkingDots()
        Spacer(Modifier.height(14.dp))
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
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedMitraMascot(
                        mood = when {
                            state.listening -> MascotMood.LISTENING
                            state.loading || state.ttsSpeaking -> MascotMood.THINKING
                            state.awaitingNext -> MascotMood.CELEBRATING
                            else -> MascotMood.IDLE
                        },
                        size = 46.dp,
                    )
                    Column(Modifier.padding(start = 8.dp)) {
                        Text("મિત્ર સાથે શીખીએ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (state.conceptTitleGujarati.isNotBlank()) {
                            Text(state.conceptTitleGujarati, style = MaterialTheme.typography.bodySmall)
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
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 180f),
                    label = "session-progress",
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )

                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width / 4 } + fadeOut())
                    },
                    label = "question-transition",
                ) { activity ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${activityEmoji(activity.type)} ${activityLabel(activity.type)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    "${state.questionIndex + 1}/${state.questions.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            activity.sourcePage?.let { Text("📖 પાનું $it", style = MaterialTheme.typography.labelMedium) }
                            Text(activity.promptGujarati, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, tonalElevation = 2.dp) {
                        IconButton(onClick = onReplayPrompt, enabled = !state.listening && state.ttsAvailable != false) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (state.ttsSpeaking) "બોલું છું" else "ફરી સાંભળો",
                            )
                        }
                    }
                    current.hintGujarati?.takeIf { it.isNotBlank() }?.let {
                        Surface(shape = CircleShape, tonalElevation = 2.dp) {
                            IconButton(onClick = onHint, enabled = !state.awaitingNext && !state.loading && !state.listening) {
                                Icon(Icons.Default.Lightbulb, contentDescription = "સંકેત")
                            }
                        }
                    }
                    if (state.ttsSpeaking) Text("મિત્ર બોલે છે…", style = MaterialTheme.typography.bodySmall)
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
                            shape = RoundedCornerShape(14.dp),
                        ) { Text(option, style = MaterialTheme.typography.titleSmall) }
                    }

                    EvaluationMode.PARTICIPATION -> if (current.type == ActivityType.TEACH_MITRA && state.answer.isNotBlank()) {
                        Text("તમે સમજાવ્યું: “${state.answer}”", style = MaterialTheme.typography.bodyMedium)
                    }

                    else -> OutlinedTextField(
                        value = state.answer,
                        onValueChange = onAnswerChange,
                        label = {
                            Text(
                                if (current.type == ActivityType.SPELLING) "શબ્દ લખો"
                                else if (current.evaluationMode == EvaluationMode.NUMERIC) "અંતિમ જવાબ"
                                else "ટૂંકો જવાબ"
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
                        shape = RoundedCornerShape(15.dp),
                    )
                }

                AnimatedVisibility(
                    visible = state.hintText != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.92f),
                    exit = fadeOut() + scaleOut(targetScale = 0.96f),
                ) {
                    state.hintText?.let {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text("💡 $it", modifier = Modifier.fillMaxWidth().padding(10.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                AnimatedVisibility(
                    visible = state.feedback != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.88f),
                    exit = fadeOut() + scaleOut(targetScale = 0.96f),
                ) {
                    state.feedback?.let {
                        Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.secondaryContainer, shadowElevation = 2.dp) {
                            Text(it, modifier = Modifier.fillMaxWidth().padding(11.dp), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                state.voiceMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (state.ttsAvailable == false) {
                    Text("આ ફોનમાં ગુજરાતી અવાજ ઉપલબ્ધ નથી; લખાણ ચાલુ રહેશે.", style = MaterialTheme.typography.bodySmall)
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                if (!state.awaitingNext) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when (current.evaluationMode) {
                            EvaluationMode.PARTICIPATION -> Button(
                                onClick = onCompleteParticipation,
                                enabled = !state.loading && !state.listening,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text(current.completionButtonGujarati) }

                            EvaluationMode.MULTIPLE_CHOICE -> Spacer(Modifier.weight(1f))

                            else -> Button(
                                onClick = onSubmit,
                                enabled = !state.loading && !state.listening,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Text(" ચકાસો")
                            }
                        }
                        OutlinedButton(onClick = onSkip, enabled = !state.loading && !state.listening) {
                            Text("છોડો")
                        }
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
                            IconButton(onClick = onStop, enabled = !state.loading) {
                                Icon(Icons.Default.StopCircle, contentDescription = "આજ માટે બસ", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    Text(
                        if (state.questionIndex == state.questions.lastIndex) "પૂર્ણ કરવા નીચેનું ✓ દબાવો."
                        else "આગળના પ્રશ્ન માટે નીચેનું તીર દબાવો.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }

        if (state.awaitingNext) {
            FloatingActionButton(
                onClick = onNext,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    if (state.questionIndex == state.questions.lastIndex) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = if (state.questionIndex == state.questions.lastIndex) "પૂર્ણ કરીએ" else "આગળનો પ્રશ્ન",
                )
            }
        }
    }
}

@Composable
private fun RoughWorkBoard(questionId: String, work: ArithmeticWork, enabled: Boolean) {
    val strokes = remember(questionId) { mutableStateListOf<List<Offset>>() }
    var expanded by remember(questionId) { mutableStateOf(true) }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val inkColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("✍️ રફ કામ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "ડાબે દશક, જમણે એકમ. ગણતરી એકમથી શરૂ કરો.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(
                    onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                    enabled = enabled && strokes.isNotEmpty(),
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "છેલ્લી લીટી દૂર કરો")
                }
                IconButton(onClick = { strokes.clear() }, enabled = enabled && strokes.isNotEmpty()) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "રફ કામ સાફ કરો")
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "રફ કામ નાનું કરો" else "રફ કામ ખોલો",
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    GuidedColumnLayout(work)
                    GuidedStepEntry(questionId = questionId, work = work, enabled = enabled)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
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
                            val grid = 28.dp.toPx()
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
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedStepEntry(
    questionId: String,
    work: ArithmeticWork,
    enabled: Boolean,
) {
    val expected = remember(questionId) { GuidedMathCoach.expected(work) } ?: return
    var ones by remember(questionId) { mutableStateOf("") }
    var regroup by remember(questionId) { mutableStateOf("") }
    var tens by remember(questionId) { mutableStateOf("") }
    var message by remember(questionId) { mutableStateOf<String?>(null) }
    var success by remember(questionId) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("પગલાંના ખાના", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GuidedDigitField(
                value = tens,
                label = "દશક",
                enabled = enabled,
                onValueChange = { tens = it; message = null; success = false },
                modifier = Modifier.weight(1f),
            )
            if (work.regrouping || expected.regroup > 0) {
                GuidedDigitField(
                    value = regroup,
                    label = expected.regroupLabelGujarati,
                    enabled = enabled,
                    onValueChange = { regroup = it; message = null; success = false },
                    modifier = Modifier.weight(1f),
                )
            }
            GuidedDigitField(
                value = ones,
                label = "એકમ",
                enabled = enabled,
                onValueChange = { ones = it; message = null; success = false },
                modifier = Modifier.weight(1f),
            )
        }
        Text("ભરવાનો ક્રમ: એકમ → કેરિ/ઉધાર → દશક", style = MaterialTheme.typography.bodySmall)
        FilledTonalButton(
            onClick = {
                val result = GuidedMathCoach.check(
                    work = work,
                    ones = GujaratiNumberNormalizer.parseInt(ones),
                    regroup = if (work.regrouping || expected.regroup > 0) {
                        GujaratiNumberNormalizer.parseInt(regroup)
                    } else {
                        expected.regroup
                    },
                    tens = GujaratiNumberNormalizer.parseInt(tens),
                )
                message = result.messageGujarati
                success = result.correct
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) { Text("પગલાં ચકાસો") }
        message?.let {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
                color = if (success) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Text(it, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun GuidedDigitField(
    value: String,
    label: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { candidate ->
            val clean = candidate.filter { it.isDigit() || it in '૦'..'૯' }.take(2)
            onValueChange(clean)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun GuidedColumnLayout(work: ArithmeticWork) {
    val topTens = (work.top / 10) % 10
    val topOnes = work.top % 10
    val bottomTens = (work.bottom / 10) % 10
    val bottomOnes = work.bottom % 10
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MathAlignedRow(operator = "", tens = "દશક", ones = "એકમ", labelRow = true)
        if (work.regrouping) {
            MathAlignedRow(operator = "", tens = "કેરિ/ઉધાર", ones = "", small = true)
        }
        MathAlignedRow(operator = "", tens = topTens.toString(), ones = topOnes.toString())
        MathAlignedRow(operator = work.operator, tens = bottomTens.toString(), ones = bottomOnes.toString())
        Text("────────", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
        MathAlignedRow(operator = "", tens = "?", ones = "?")
    }
}

@Composable
private fun MathAlignedRow(
    operator: String,
    tens: String,
    ones: String,
    small: Boolean = false,
    labelRow: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(30.dp, 48.dp), contentAlignment = Alignment.Center) {
            if (operator.isNotBlank()) {
                Text(operator, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (labelRow) {
            PlaceValueLabel(tens)
            PlaceValueLabel(ones)
        } else {
            MathCell(tens, small = small)
            MathCell(ones, small = small)
        }
    }
}

@Composable
private fun PlaceValueLabel(text: String) {
    Box(modifier = Modifier.size(52.dp, 30.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MathCell(text: String, small: Boolean = false) {
    Surface(
        modifier = Modifier.size(52.dp, if (small) 38.dp else 48.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (small) FontWeight.Normal else FontWeight.Bold,
            )
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

    Surface(modifier = gestureModifier, shape = RoundedCornerShape(16.dp), tonalElevation = if (state.listening) 5.dp else 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedScale(active = state.listening) {
                Icon(
                    if (state.listening) Icons.Default.StopCircle else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = if (state.listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        !state.speechInputAvailable -> "આ ફોનમાં voice input ઉપલબ્ધ નથી"
                        state.listening -> "બોલો… છોડશો ત્યારે ચકાસીશ"
                        else -> "દબાવી રાખીને જવાબ બોલો"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                if (state.partialTranscript.isNotBlank()) {
                    Text("“${state.partialTranscript}”", style = MaterialTheme.typography.bodyMedium)
                }
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
        AnimatedMitraMascot(mood = MascotMood.CELEBRATING, size = 124.dp)
        Text("🌟 આજની રમત પૂરી!", style = MaterialTheme.typography.headlineLarge)
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
