package com.mitra.learning.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mitra.learning.study.practice.SpokenAnswerNormalizer
import com.mitra.learning.learning.evaluation.EnglishSentenceEvaluator
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.MascotMood
import com.mitra.learning.ui.animation.SuccessBurst
import com.mitra.learning.voice.SpeechInput
import com.mitra.learning.voice.SpeechInputState
import com.mitra.learning.voice.SpeechOutput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SentencePuzzle(
    val emoji: String,
    val pictureDescription: String,
    val hintGujarati: String,
    val grammarHelp: String,
    val sentence: String,
) {
    val words: List<String> = sentence.split(' ')
}

private val sentencePuzzles = listOf(
    SentencePuzzle("🔴", "નજીકનો લાલ બોલ", "ચિત્ર વિશે English sentence બોલો.", "નજીકની એક વસ્તુ: This is a…", "This is a red ball"),
    SentencePuzzle("🍎", "દૂરનું સફરજન", "દૂરની વસ્તુ વિશે sentence બોલો.", "દૂરની એક વસ્તુ: That is an…", "That is an apple"),
    SentencePuzzle("📘  🎒", "નજીકનું પુસ્તક અને દૂરની બેગ", "બે વસ્તુને and થી જોડો.", "This… and that…", "This is a book and that is a bag"),
    SentencePuzzle("🐱  🐶", "બિલાડી અને કૂતરો", "બે animals વિશે sentence બનાવો.", "બે વસ્તુ માટે are", "A cat and a dog are animals"),
    SentencePuzzle("🍊", "નજીકનું નારંગી", "orange પહેલાં a કે an?", "vowel sound પહેલાં an", "This is an orange"),
    SentencePuzzle("🪁", "દૂરનો પતંગ", "દૂરના kite વિશે બોલો.", "That is a…", "That is a kite"),
    SentencePuzzle("☂️", "નજીકની છત્રી", "umbrella વિશે sentence બોલો.", "vowel sound પહેલાં an", "This is an umbrella"),
    SentencePuzzle("👦  👧", "છોકરો અને છોકરી", "બંને વિશે sentence બનાવો.", "and સાથે are", "A boy and a girl are friends"),
    SentencePuzzle("🖊️  🧽", "નજીકનું pen અને દૂરનું eraser", "this અને that વાપરો.", "This… and that…", "This is a pen and that is an eraser"),
    SentencePuzzle("🚙", "દૂરની blue car", "રંગ સાથે sentence બોલો.", "That is a + colour + object", "That is a blue car"),
    SentencePuzzle("🌼", "નજીકનું yellow flower", "રંગ સાથે sentence બોલો.", "This is a + colour + object", "This is a yellow flower"),
    SentencePuzzle("🍎  🍊", "apple અને orange", "બે fruits વિશે બોલો.", "an + and + are", "An apple and an orange are fruits"),
    SentencePuzzle("🐦", "ઝાડ પર પક્ષી", "bird ક્યાં છે તે બોલો.", "The + object + is + place", "The bird is on the tree"),
    SentencePuzzle("🐟", "પાણીમાં માછલી", "fish ક્યાં રહે છે તે બોલો.", "The + object + is + place", "The fish is in the water"),
    SentencePuzzle("👧📖", "છોકરી પુસ્તક વાંચે છે", "action word વાપરો.", "The girl is + verb-ing", "The girl is reading a book"),
    SentencePuzzle("👦⚽", "છોકરો દડાથી રમે છે", "action sentence બોલો.", "The boy is + verb-ing", "The boy is playing with a ball"),
    SentencePuzzle("🌞", "તેજસ્વી સૂર્ય", "sun વિશે sentence બનાવો.", "The + object + is + adjective", "The sun is bright"),
    SentencePuzzle("🌳", "લીલું ઝાડ", "colour word વાપરો.", "The + object + is + colour", "The tree is green"),
    SentencePuzzle("🥛", "એક ગ્લાસ દૂધ", "milk વિશે sentence બોલો.", "This is a glass of…", "This is a glass of milk"),
    SentencePuzzle("🏫", "અમારી શાળા", "school વિશે sentence બોલો.", "This is my…", "This is my school"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SentenceBuilderScreen(
    speechInput: SpeechInput,
    speechOutput: SpeechOutput,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var index by remember { mutableIntStateOf(0) }
    val puzzle = sentencePuzzles[index % sentencePuzzles.size]
    var answer by remember(index) { mutableStateOf("") }
    var selectedWords by remember(index) { mutableStateOf(emptyList<String>()) }
    var showHelp by remember(index) { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var message by remember(index) { mutableStateOf<String?>(null) }
    var correct by remember(index) { mutableStateOf(false) }
    var micGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    fun check(candidate: String) {
        val expected = SpokenAnswerNormalizer.text(puzzle.sentence)
        val actual = SpokenAnswerNormalizer.text(candidate)
        val evaluation = EnglishSentenceEvaluator.evaluate(expected, actual)
        correct = evaluation.correct
        message = if (correct) {
            listOf("શાબાશ! સુંદર sentence.", "Perfect! બહુ સરસ બોલ્યા.", "વાહ! Grammar અને શબ્દક્રમ બંને સાચા.")[(index + actual.length) % 3]
        } else {
            val missing = evaluation.missingWords.take(3)
            if (missing.isEmpty()) {
                "Sentence લગભગ સાચું છે. શબ્દક્રમ ફરી સાંભળો અને એક વાર વધુ બોલો."
            } else {
                "આ શબ્દો ઉમેરવાનો પ્રયત્ન કરો: ${missing.joinToString(", ")}. Help words જોઈ શકો."
            }
        }
        if (correct) {
            scope.launch {
                speechOutput.speak(message.orEmpty(), "gu-IN")
                delay(1_700)
                index = (index + 1) % sentencePuzzles.size
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
        if (granted) scope.launch { speechInput.startListening("en-IN") }
        else message = "માઇક્રોફોનની પરવાનગી આપો અથવા sentence લખો."
    }

    LaunchedEffect(speechInput) {
        speechInput.state.collect { state ->
            when (state) {
                SpeechInputState.Idle -> listening = false
                SpeechInputState.Listening -> { listening = true; message = "હું સાંભળું છું…" }
                is SpeechInputState.Partial -> { listening = true; answer = state.text }
                is SpeechInputState.Result -> { listening = false; answer = state.text; check(state.text) }
                is SpeechInputState.Error -> { listening = false; message = state.messageGujarati }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { speechInput.cancel(); speechOutput.stop() }
    }

    AnimatedLearningBackground(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "પાછા") }
                AnimatedMitraMascot(mood = if (correct) MascotMood.CELEBRATING else if (listening) MascotMood.LISTENING else MascotMood.ENCOURAGING, size = 52.dp)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("English Sentence Builder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("ચિત્ર જુઓ • sentence બોલો • help words", style = MaterialTheme.typography.bodySmall)
                }
                Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("${index + 1}/${sentencePuzzles.size}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            LinearProgressIndicator(progress = { (index + 1f) / sentencePuzzles.size }, modifier = Modifier.fillMaxWidth())

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(puzzle.emoji, style = MaterialTheme.typography.displayLarge)
                    Text(puzzle.pictureDescription, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(puzzle.hintGujarati)

                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it; correct = false; message = null },
                        label = { Text("Speak or type the full sentence") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                if (!micGranted) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                else scope.launch {
                                    if (listening) speechInput.stopListening() else speechInput.startListening("en-IN")
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Icon(Icons.Default.Mic, null); Text(if (listening) " Stop" else " Sentence બોલો") }
                        Button(onClick = { check(answer) }, enabled = answer.isNotBlank(), modifier = Modifier.weight(1f)) { Text("ચકાસો") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showHelp = !showHelp }) { Icon(Icons.Default.HelpOutline, null); Text(" Help") }
                        OutlinedButton(onClick = { scope.launch { speechOutput.speak(puzzle.sentence, "en-IN") } }) { Icon(Icons.Default.VolumeUp, null); Text(" સાંભળો") }
                        IconButton(onClick = { answer = ""; selectedWords = emptyList(); message = null; correct = false }) { Icon(Icons.Default.Refresh, "ફરી") }
                    }

                    AnimatedVisibility(showHelp) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("નિયમ: ${puzzle.grammarHelp}", Modifier.fillMaxWidth().padding(10.dp))
                            }
                            Text("Help words — સાચા ક્રમમાં દબાવો:", fontWeight = FontWeight.SemiBold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                puzzle.words.forEachIndexed { wordIndex, word ->
                                    AssistChip(
                                        onClick = {
                                            val updated = selectedWords + word
                                            selectedWords = updated
                                            answer = updated.joinToString(" ")
                                        },
                                        label = { Text(word) },
                                        enabled = wordIndex == selectedWords.size,
                                    )
                                }
                            }
                        }
                    }

                    message?.let {
                        Surface(shape = RoundedCornerShape(14.dp), color = if (correct) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(it, Modifier.fillMaxWidth().padding(11.dp), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        SuccessBurst(trigger = correct, modifier = Modifier.fillMaxSize())
    }
}
