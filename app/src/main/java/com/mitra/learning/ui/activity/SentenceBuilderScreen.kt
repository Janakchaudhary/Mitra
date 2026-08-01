package com.mitra.learning.ui.activity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.MascotMood
import com.mitra.learning.ui.animation.SuccessBurst

private data class SentencePuzzle(
    val emoji: String,
    val hintGujarati: String,
    val words: List<String>,
)

private val sentencePuzzles = listOf(
    SentencePuzzle("🔴", "આ નજીકની વસ્તુ બતાવીને કહો.", listOf("This", "is", "a", "red", "ball.")),
    SentencePuzzle("🍎", "દૂરની apple વિશે કહો.", listOf("That", "is", "an", "apple.")),
    SentencePuzzle("📘 🎒", "બે વસ્તુને and થી જોડો.", listOf("This", "is", "a", "book", "and", "that", "is", "a", "bag.")),
    SentencePuzzle("🐱 🐶", "બે animals વિશે એક વાક્ય બનાવો.", listOf("A", "cat", "and", "a", "dog", "are", "animals.")),
    SentencePuzzle("🍊", "orange પહેલાં a કે an?", listOf("This", "is", "an", "orange.")),
)

@Composable
fun SentenceBuilderScreen(onBack: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val puzzle = sentencePuzzles[index % sentencePuzzles.size]
    var selected by remember(index) { mutableStateOf(listOf<String>()) }
    var message by remember(index) { mutableStateOf<String?>(null) }
    val remaining = puzzle.words.toMutableList().also { list -> selected.forEach { list.remove(it) } }
    val success = selected == puzzle.words && message?.startsWith("સાચું") == true

    AnimatedLearningBackground(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                AnimatedMitraMascot(
                    mood = if (success) MascotMood.CELEBRATING else MascotMood.ENCOURAGING,
                    size = 56.dp,
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text("Sentence Builder", style = MaterialTheme.typography.headlineMedium)
                    Text("this • that • and • is • are • a • an")
                }
            }

            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                },
                label = "sentence-puzzle",
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(puzzle.emoji, style = MaterialTheme.typography.displayMedium)
                        Text(puzzle.hintGujarati, style = MaterialTheme.typography.titleMedium)

                        Surface(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            shape = RoundedCornerShape(18.dp),
                            color = if (success) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                if (selected.isEmpty()) "અહીં sentence બનાવો…" else selected.joinToString(" "),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }

                        WordChips(remaining.shuffled(seed = index + 17)) { word ->
                            selected = selected + word
                            message = null
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { if (selected.isNotEmpty()) selected = selected.dropLast(1) },
                                enabled = selected.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Default.Backspace, contentDescription = null)
                                Text(" પાછું")
                            }
                            Button(
                                onClick = {
                                    if (selected == puzzle.words) message = "સાચું sentence! ⭐"
                                    else message = "ફરી ગોઠવો. Capital letter થી શરૂ કરો અને sentence નો અર્થ જુઓ."
                                },
                                enabled = selected.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                            ) { Text("ચેક કરો") }
                        }

                        AnimatedVisibility(
                            visible = message != null,
                            enter = fadeIn() + scaleIn(initialScale = 0.9f),
                            exit = fadeOut() + scaleOut(targetScale = 0.96f),
                        ) {
                            message?.let {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (success) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                ) {
                                    Text(it, modifier = Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        if (success) {
                            Button(
                                onClick = { index += 1 },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                            ) { Text("આગળનું sentence") }
                        }
                    }
                }
            }
        }
        SuccessBurst(
            trigger = if (success) "sentence-$index" else null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun WordChips(words: List<String>, onWord: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        words.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { word ->
                    OutlinedButton(
                        onClick = { onWord(word) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(word) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun <T> List<T>.shuffled(seed: Int): List<T> = toMutableList().also { list ->
    val random = java.util.Random(seed.toLong())
    for (i in list.lastIndex downTo 1) {
        val j = random.nextInt(i + 1)
        val tmp = list[i]
        list[i] = list[j]
        list[j] = tmp
    }
}
