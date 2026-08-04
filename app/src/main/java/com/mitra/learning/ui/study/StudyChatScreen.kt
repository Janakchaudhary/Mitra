package com.mitra.learning.ui.study

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.AnimatedScale
import com.mitra.learning.ui.animation.MascotMood
import com.mitra.learning.ui.animation.ThinkingDots
import com.mitra.learning.study.practice.MitraPracticeTopic

@Composable
fun StudyChatScreen(
    state: StudyChatUiState,
    onInput: (String) -> Unit,
    onAsk: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onMicDenied: () -> Unit,
    onHandsFreeChange: (Boolean) -> Unit,
    onStartPractice: (MitraPracticeTopic, Int) -> Unit,
    onStopPractice: () -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    AnimatedLearningBackground(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .22f)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            AnimatedMitraMascot(
                mood = when {
                    state.listening -> MascotMood.LISTENING
                    state.loading -> MascotMood.THINKING
                    else -> MascotMood.IDLE
                },
                size = 54.dp,
            )
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("મિત્રને પૂછીએ", style = MaterialTheme.typography.titleLarge)
                Text("પુસ્તક • ઘડિયા • સંખ્યા • spelling • voice રમત", style = MaterialTheme.typography.bodySmall)
                state.remainingSeconds?.let { seconds ->
                    val min = seconds / 60
                    val sec = seconds % 60
                    Text("બાકી સમય %02d:%02d".format(min, sec), style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onReplay) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Replay answer")
            }
        }

        PracticeTopicBar(
            state = state,
            onStartPractice = onStartPractice,
            onStopPractice = onStopPractice,
        )

        if (!state.preparedBooksAvailable) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    "પુસ્તકના જવાબ માટે Parent mode માં પાઠ Prepare કરો. ગણિત અને મોબાઇલ-ગેમ વિશેનું માર્ગદર્શન હમણાં પણ ચાલશે.",
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
                        AnimatedMitraMascot(mood = MascotMood.THINKING, size = 42.dp)
                        ThinkingDots()
                        Text("  વિચારું છું…")
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
            onHandsFreeChange = onHandsFreeChange,
        )
        }
    }
}

@Composable
private fun PracticeTopicBar(
    state: StudyChatUiState,
    onStartPractice: (MitraPracticeTopic, Int) -> Unit,
    onStopPractice: () -> Unit,
) {
    var selectedLength by remember { mutableStateOf(20) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("મિત્ર મને પ્રશ્ન પૂછે", style = MaterialTheme.typography.titleMedium)
                    if (state.practiceTopic != null) {
                        Text(
                            "પ્રશ્ન ${state.practiceAnswered.coerceAtMost(state.practiceTarget)}/${state.practiceTarget} • સાચા ${state.correctCount}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                if (state.activeChallenge != null) {
                    FilledTonalButton(onClick = onStopPractice, enabled = !state.loading) {
                        Text("રોકો")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("રમતની લંબાઈ:", style = MaterialTheme.typography.labelMedium)
                listOf(20, 25).forEach { length ->
                    FilterChip(
                        selected = selectedLength == length,
                        onClick = { selectedLength = length },
                        label = { Text("$length પ્રશ્ન") },
                        enabled = state.activeChallenge == null && !state.loading,
                    )
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            ) {
                items(MitraPracticeTopic.entries, key = { it.name }) { topic ->
                    FilledTonalButton(
                        onClick = { onStartPractice(topic, selectedLength) },
                        enabled = !state.loading && !state.timeLimitReached,
                    ) {
                        Text("${topic.emoji} ${topic.titleGujarati}")
                    }
                }
            }
            state.activeChallenge?.let { challenge ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("હાલનો પ્રશ્ન", style = MaterialTheme.typography.labelLarge)
                        Text(challenge.promptGujarati, style = MaterialTheme.typography.titleMedium)
                        if (state.challengeAttempts > 0) {
                            Text("ફરી પ્રયાસ ${state.challengeAttempts + 1}/2", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
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
            Text("🎤 પૂછો અથવા voice રમત રમો!", style = MaterialTheme.typography.headlineSmall)
            Text("પુસ્તક: ‘આ પાઠમાં હાથી વિશે શું લખ્યું છે?’")
            Text("ગણિત: ‘૫ નો ઘડિયો કહો’ અથવા ‘૩૮ પછી શું આવે?’")
            Text("રમત: ‘મને spelling પૂછો’ અથવા ઉપરથી વિષય પસંદ કરો.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MessageBubble(message: StudyMessage) {
    var visible by remember(message.id) { mutableStateOf(false) }
    LaunchedEffect(message.id) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { fullHeight -> fullHeight / 3 },
    ) {
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
                shadowElevation = 2.dp,
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (message.speaker == StudySpeaker.CHILD) "તમે" else "🦁 મિત્ર", style = MaterialTheme.typography.labelLarge)
                    message.responseKind?.let { kind ->
                        Text(
                            when (kind) {
                                com.mitra.learning.study.StudyResponseKind.TEXTBOOK -> "📖 પુસ્તક પરથી"
                                com.mitra.learning.study.StudyResponseKind.LOCAL_MATH -> "🧮 સ્થાનિક ગણિત સમજણ"
                                com.mitra.learning.study.StudyResponseKind.LOCAL_GUIDANCE -> "🌱 મિત્રનું માર્ગદર્શન"
                                com.mitra.learning.study.StudyResponseKind.VOICE_PRACTICE -> "🎤 voice પ્રશ્ન રમત"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(message.text, style = MaterialTheme.typography.bodyLarge)
                    if (message.sources.isNotEmpty()) {
                        Text("📖 ${message.sources.joinToString(" • ")}", style = MaterialTheme.typography.labelSmall)
                    }
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
    onHandsFreeChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var pendingHandsFree by remember { mutableStateOf(false) }
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) {
            if (pendingHandsFree) onHandsFreeChange(true) else onStartVoice()
        } else onMicDenied()
        pendingHandsFree = false
    }

    Surface(tonalElevation = 4.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = {
                    if (state.handsFreeEnabled) onHandsFreeChange(false)
                    else if (!granted) {
                        pendingHandsFree = true
                        launcher.launch(Manifest.permission.RECORD_AUDIO)
                    } else onHandsFreeChange(true)
                },
                enabled = state.speechAvailable && !state.loading && !state.timeLimitReached,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    if (state.handsFreeEnabled) Icons.Default.StopCircle else Icons.Default.Mic,
                    contentDescription = null,
                )
                Text(
                    when {
                        state.handsFreeEnabled && state.listening -> "  લાઇવ વાત ચાલુ • મિત્ર સાંભળે છે"
                        state.handsFreeEnabled && state.waitingForNextTurn -> "  લાઇવ વાત ચાલુ • આગળ બોલો"
                        state.handsFreeEnabled -> "  લાઇવ વાત બંધ કરો"
                        else -> "  લાઇવ વાત શરૂ કરો"
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInput,
                    placeholder = { Text(if (state.activeChallenge != null) "જવાબ બોલો અથવા લખો…" else "મારો સવાલ…") },
                    modifier = Modifier.weight(1f),
                    enabled = !state.loading && !state.timeLimitReached,
                    maxLines = 3,
                )
                IconButton(
                    enabled = state.speechAvailable && !state.loading && !state.timeLimitReached,
                    onClick = {
                        if (!granted) {
                            pendingHandsFree = false
                            launcher.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (state.listening) onStopVoice() else onStartVoice()
                    },
                ) {
                    AnimatedScale(active = state.listening) {
                        Icon(
                            if (state.listening) Icons.Default.StopCircle else Icons.Default.Mic,
                            contentDescription = "Voice question",
                            tint = if (state.listening) MaterialTheme.colorScheme.error else Color.Unspecified,
                        )
                    }
                }
                Button(onClick = onAsk, enabled = state.input.isNotBlank() && !state.loading && !state.timeLimitReached) {
                    Icon(Icons.Default.Send, contentDescription = null)
                }
            }
        }
    }
}
