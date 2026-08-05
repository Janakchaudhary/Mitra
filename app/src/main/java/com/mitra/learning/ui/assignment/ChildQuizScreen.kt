package com.mitra.learning.ui.assignment

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.MascotMood
import com.mitra.learning.ui.animation.SuccessBurst

@Composable
fun ChildQuizScreen(
    state: ChildQuizUiState,
    onAnswer: (String) -> Unit,
    onSubmit: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var micGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
        if (granted) onStartVoice()
    }

    AnimatedLearningBackground(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.loading -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            state.plan == null -> Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedMitraMascot(mood = MascotMood.ENCOURAGING, size = 90.dp)
                Text(state.error ?: "કસોટી ઉપલબ્ધ નથી.", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("પાછા") }
            }
            else -> {
                val plan = state.plan
                val question = state.currentQuestion
                Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "પાછા") }
                        AnimatedMitraMascot(
                            mood = when { state.listening -> MascotMood.LISTENING; state.awaitingNext -> MascotMood.CELEBRATING; else -> MascotMood.ENCOURAGING },
                            size = 52.dp,
                        )
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(plan.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            plan.chapterTitleGujarati?.let { chapter ->
                                Text(
                                    listOfNotNull(plan.bookTitle, chapter).joinToString(" → "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text("દરેક પ્રશ્ન 1 ગુણ • ગુણ ${state.marks}/${plan.questions.size}")
                        }
                        IconButton(onClick = onReplay, enabled = !state.completed) { Icon(Icons.Default.VolumeUp, "પ્રશ્ન સાંભળો") }
                    }
                    LinearProgressIndicator(
                        progress = { if (plan.questions.isEmpty()) 0f else (state.questionIndex + if (state.completed) 1 else 0).toFloat() / plan.questions.size },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (state.completed) {
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        ) {
                            Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text("🏆", style = MaterialTheme.typography.displayLarge)
                                Text("${state.marks}/${plan.questions.size} ગુણ", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                Text(state.feedback.orEmpty(), style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = onBack) { Text("Home") }
                            }
                        }
                    } else if (question != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("પ્રશ્ન ${state.questionIndex + 1}/${plan.questions.size} • 1 ગુણ", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontWeight = FontWeight.Bold)
                                }
                                Text(question.promptGujarati, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                question.sourceLabels.takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(), style = MaterialTheme.typography.bodySmall) }
                                OutlinedTextField(
                                    value = state.answer,
                                    onValueChange = onAnswer,
                                    label = { Text("જવાબ બોલો અથવા લખો") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.awaitingNext,
                                    minLines = 2,
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            if (!micGranted) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            else if (state.listening) onStopVoice() else onStartVoice()
                                        },
                                        enabled = !state.awaitingNext,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(if (state.listening) Icons.Default.StopCircle else Icons.Default.Mic, null)
                                        Text(if (state.listening) " રોકો" else " બોલો")
                                    }
                                    Button(onClick = onSubmit, enabled = state.answer.isNotBlank() && !state.awaitingNext, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Send, null); Text(" જવાબ")
                                    }
                                }
                                state.feedback?.let {
                                    Surface(shape = RoundedCornerShape(14.dp), color = if (state.awaitingNext && state.attempts <= 1) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer) {
                                        Text(it, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
                SuccessBurst(trigger = state.awaitingNext && state.attempts <= 1, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
