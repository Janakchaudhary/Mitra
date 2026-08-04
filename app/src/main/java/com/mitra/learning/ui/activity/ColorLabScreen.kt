package com.mitra.learning.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mitra.learning.study.practice.SpokenAnswerNormalizer
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.MascotMood
import com.mitra.learning.ui.animation.SuccessBurst
import com.mitra.learning.voice.SpeechInput
import com.mitra.learning.voice.SpeechInputState
import com.mitra.learning.voice.SpeechOutput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class LearnColor(val gujarati: String, val english: String, val color: Color)
private data class ColorObject(val emoji: String, val gujarati: String, val english: String)
private enum class ColorTask { PICK, SAY_GUJARATI, SAY_ENGLISH, SPELL }
private const val TOTAL_COLOR_ROUNDS = 20

private val learnColors = listOf(
    LearnColor("લાલ", "red", Color(0xFFE74C3C)),
    LearnColor("વાદળી", "blue", Color(0xFF3F7FDB)),
    LearnColor("લીલો", "green", Color(0xFF4FAE58)),
    LearnColor("પીળો", "yellow", Color(0xFFF2C94C)),
    LearnColor("નારંગી", "orange", Color(0xFFF2994A)),
    LearnColor("જાંબલી", "purple", Color(0xFF8E5CC7)),
    LearnColor("ગુલાબી", "pink", Color(0xFFF279A9)),
    LearnColor("ભૂરો", "brown", Color(0xFF8D6748)),
)
private val colorObjects = listOf(
    ColorObject("🎈", "ફુગ્ગો", "balloon"), ColorObject("🍎", "સફરજન", "apple"),
    ColorObject("🪁", "પતંગ", "kite"), ColorObject("🐟", "માછલી", "fish"),
    ColorObject("🌼", "ફૂલ", "flower"), ColorObject("🚗", "ગાડી", "car"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorLabScreen(
    speechInput: SpeechInput,
    speechOutput: SpeechOutput,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var round by remember { mutableIntStateOf(0) }
    val target = learnColors[round % learnColors.size]
    val item = colorObjects[round % colorObjects.size]
    var task by remember(round) { mutableStateOf(ColorTask.PICK) }
    var picked by remember(round) { mutableStateOf<LearnColor?>(null) }
    var answer by remember(round, task) { mutableStateOf("") }
    var message by remember(round, task) { mutableStateOf<String?>(null) }
    var correct by remember(round, task) { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var micGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    fun expected(): String = when (task) {
        ColorTask.PICK -> target.gujarati
        ColorTask.SAY_GUJARATI -> target.gujarati
        ColorTask.SAY_ENGLISH, ColorTask.SPELL -> target.english
    }

    fun advance() {
        when (task) {
            ColorTask.PICK -> task = ColorTask.SAY_GUJARATI
            ColorTask.SAY_GUJARATI -> task = ColorTask.SAY_ENGLISH
            ColorTask.SAY_ENGLISH -> task = ColorTask.SPELL
            ColorTask.SPELL -> round = (round + 1) % TOTAL_COLOR_ROUNDS
        }
    }

    fun check(candidate: String) {
        val actual = if (task == ColorTask.SPELL) {
            SpokenAnswerNormalizer.spelling(candidate)
        } else {
            SpokenAnswerNormalizer.text(candidate).replace(" ", "")
        }
        val exp = SpokenAnswerNormalizer.text(expected()).replace(" ", "")
        correct = actual == exp
        message = if (correct) {
            listOf("વાહ! રંગ બરાબર ઓળખ્યો.", "શાબાશ! ગુજરાતી અને English બંને સરસ.", "Perfect colour answer!")[(round + task.ordinal) % 3]
        } else {
            when (task) {
                ColorTask.SAY_GUJARATI -> "નમૂનાને જુઓ. આ રંગનું નામ '${target.gujarati}' છે. ફરી બોલો."
                ColorTask.SAY_ENGLISH -> "English માં આ રંગ '${target.english}' કહેવાય. ફરી બોલો."
                ColorTask.SPELL -> "ધીમે અક્ષર-અક્ષર બોલો: ${target.english.toCharArray().joinToString(" ") { it.uppercase() }}"
                ColorTask.PICK -> "નમૂના જેવો જ રંગ પસંદ કરો."
            }
        }
        if (correct) scope.launch {
            speechOutput.speak(message.orEmpty(), "gu-IN")
            delay(1_350)
            correct = false
            answer = ""
            message = null
            advance()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
        if (granted) scope.launch { speechInput.startListening(if (task == ColorTask.SAY_GUJARATI) "gu-IN" else "en-IN") }
        else message = "માઇક્રોફોનની પરવાનગી આપો અથવા જવાબ લખો."
    }

    LaunchedEffect(speechInput) {
        speechInput.state.collect { state ->
            when (state) {
                SpeechInputState.Idle -> listening = false
                SpeechInputState.Listening -> listening = true
                is SpeechInputState.Partial -> { listening = true; answer = state.text }
                is SpeechInputState.Result -> { listening = false; answer = state.text; check(state.text) }
                is SpeechInputState.Error -> { listening = false; message = state.messageGujarati }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { speechInput.cancel(); speechOutput.stop() } }

    AnimatedLearningBackground(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "પાછા") }
                AnimatedMitraMascot(
                    mood = if (correct) MascotMood.CELEBRATING else if (listening) MascotMood.LISTENING else MascotMood.ENCOURAGING,
                    size = 52.dp,
                )
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("રંગોની મજા", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("રંગ પસંદ કરો • બોલો • English • spelling", style = MaterialTheme.typography.bodySmall)
                }
                Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("${round + 1}/$TOTAL_COLOR_ROUNDS", Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            LinearProgressIndicator(progress = { (round + 1) / TOTAL_COLOR_ROUNDS.toFloat() }, modifier = Modifier.fillMaxWidth())

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(item.emoji, style = MaterialTheme.typography.displayLarge)
                    Text("${item.gujarati} • ${item.english}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("રંગનો નમૂનો")
                        Box(Modifier.size(42.dp).background(target.color, CircleShape).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
                    }
                    Text(
                        when (task) {
                            ColorTask.PICK -> "નમૂના જેવો રંગ પસંદ કરો."
                            ColorTask.SAY_GUJARATI -> "આ રંગનું ગુજરાતી નામ બોલો."
                            ColorTask.SAY_ENGLISH -> "Now say the colour in English."
                            ColorTask.SPELL -> "Spell ${target.english.uppercase()} letter by letter."
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )

                    if (task == ColorTask.PICK) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            val options = (listOf(target) + learnColors.filterNot { it == target }
                                .shuffled(kotlin.random.Random(round)).take(5))
                                .shuffled(kotlin.random.Random(round + 71))
                            options.forEach { option ->
                                Box(
                                    modifier = Modifier.size(54.dp).background(option.color, CircleShape)
                                        .border(if (picked == option) 4.dp else 2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .clickable {
                                            picked = option
                                            check(option.gujarati)
                                        },
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it; message = null; correct = false },
                            label = { Text("જવાબ બોલો અથવા લખો") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    if (!micGranted) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    else scope.launch { if (listening) speechInput.stopListening() else speechInput.startListening(if (task == ColorTask.SAY_GUJARATI) "gu-IN" else "en-IN") }
                                },
                                modifier = Modifier.weight(1f),
                            ) { Icon(Icons.Default.Mic, null); Text(if (listening) " Stop" else " બોલો") }
                            Button(onClick = { check(answer) }, enabled = answer.isNotBlank(), modifier = Modifier.weight(1f)) { Text("ચકાસો") }
                        }
                        AssistChip(
                            onClick = { scope.launch { speechOutput.speak(if (task == ColorTask.SAY_GUJARATI) target.gujarati else target.english, if (task == ColorTask.SAY_GUJARATI) "gu-IN" else "en-IN") } },
                            label = { Text("સાચું ઉચ્ચારણ સાંભળો") },
                            leadingIcon = { Icon(Icons.Default.VolumeUp, null) },
                        )
                    }

                    AnimatedVisibility(message != null) {
                        Surface(shape = RoundedCornerShape(14.dp), color = if (correct) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(message.orEmpty(), Modifier.fillMaxWidth().padding(11.dp), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        SuccessBurst(trigger = correct, modifier = Modifier.fillMaxSize())
    }
}
