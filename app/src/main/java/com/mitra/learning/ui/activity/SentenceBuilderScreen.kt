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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.MascotMood
import com.mitra.learning.ui.animation.SuccessBurst

private data class SentencePuzzle(
    val emoji: String,
    val hintGujarati: String,
    val focusGujarati: String,
    val words: List<String>,
)

private val sentencePuzzles = listOf(
    SentencePuzzle("🔴", "નજીકની વસ્તુ વિશે કહો.", "This + is + a", listOf("This", "is", "a", "red", "ball.")),
    SentencePuzzle("🍎", "દૂરના apple વિશે કહો.", "That + is + an", listOf("That", "is", "an", "apple.")),
    SentencePuzzle("📘 🎒", "બે વસ્તુને and થી જોડો.", "this • that • and", listOf("This", "is", "a", "book", "and", "that", "is", "a", "bag.")),
    SentencePuzzle("🐱 🐶", "બે animals વિશે એક વાક્ય બનાવો.", "and + are", listOf("A", "cat", "and", "a", "dog", "are", "animals.")),
    SentencePuzzle("🍊", "orange પહેલાં a કે an?", "an before vowel sound", listOf("This", "is", "an", "orange.")),
    SentencePuzzle("🪁", "દૂરના kite વિશે કહો.", "That + is + a", listOf("That", "is", "a", "kite.")),
    SentencePuzzle("☂️", "નજીકની umbrella વિશે કહો.", "an before vowel sound", listOf("This", "is", "an", "umbrella.")),
    SentencePuzzle("👦 👧", "boy અને girl વિશે કહો.", "and + are", listOf("A", "boy", "and", "a", "girl", "are", "friends.")),
    SentencePuzzle("🖊️ 🧽", "નજીકનું pen અને દૂરનું eraser જોડો.", "this • that • and", listOf("This", "is", "a", "pen", "and", "that", "is", "an", "eraser.")),
    SentencePuzzle("🍎 🍊", "બે fruits વિશે કહો.", "an + and + are", listOf("An", "apple", "and", "an", "orange", "are", "fruits.")),
    SentencePuzzle("🚙", "દૂરની blue car વિશે કહો.", "That + is + a", listOf("That", "is", "a", "blue", "car.")),
    SentencePuzzle("🌼", "નજીકના yellow flower વિશે કહો.", "This + is + a", listOf("This", "is", "a", "yellow", "flower.")),
)

@Composable
fun SentenceBuilderScreen(onBack: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val puzzle = sentencePuzzles[index % sentencePuzzles.size]
    var selected by remember(index) { mutableStateOf(listOf<String>()) }
    var message by remember(index) { mutableStateOf<String?>(null) }
    val success = selected == puzzle.words && message?.startsWith("સાચું") == true

    AnimatedLearningBackground(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "પાછા")
                }
                AnimatedMitraMascot(
                    mood = if (success) MascotMood.CELEBRATING else MascotMood.ENCOURAGING,
                    size = 50.dp,
                )
                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                    Text("Sentence Builder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("this • that • and • is • are • a • an", style = MaterialTheme.typography.bodySmall)
                }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        "${index % sentencePuzzles.size + 1}/${sentencePuzzles.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (index % sentencePuzzles.size + 1).toFloat() / sentencePuzzles.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                },
                label = "sentence-puzzle",
            ) { targetIndex ->
                val animatedPuzzle = sentencePuzzles[targetIndex % sentencePuzzles.size]
                val animatedRemaining = animatedPuzzle.words.toMutableList().also { list ->
                    selected.forEach { list.remove(it) }
                }
                val animatedSuccess = selected == animatedPuzzle.words && message?.startsWith("સાચું") == true

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(animatedPuzzle.emoji, style = MaterialTheme.typography.displaySmall)
                            Column(Modifier.weight(1f)) {
                                Text(animatedPuzzle.hintGujarati, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "આજનો નિયમ: ${animatedPuzzle.focusGujarati}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (animatedSuccess) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Text("તમારું sentence", style = MaterialTheme.typography.labelLarge)
                                if (selected.isEmpty()) {
                                    Text(
                                        "નીચેના words ને સાચા ક્રમમાં દબાવો…",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    SelectedWordChips(selected) { removeIndex ->
                                        selected = selected.toMutableList().also { it.removeAt(removeIndex) }
                                        message = null
                                    }
                                }
                            }
                        }

                        Text("બાકી words", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge)
                        WordChips(animatedRemaining.shuffled(seed = targetIndex + 17)) { word ->
                            selected = selected + word
                            message = null
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { if (selected.isNotEmpty()) selected = selected.dropLast(1) },
                                enabled = selected.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Default.Backspace, contentDescription = null)
                                Text(" છેલ્લો word")
                            }
                            Button(
                                onClick = {
                                    message = if (selected == animatedPuzzle.words) {
                                        "સાચું sentence! ⭐"
                                    } else {
                                        sentenceHint(animatedPuzzle.words, selected)
                                    }
                                },
                                enabled = selected.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("ચેક કરો") }
                        }

                        AnimatedVisibility(
                            visible = message != null,
                            enter = fadeIn() + scaleIn(initialScale = 0.9f),
                            exit = fadeOut() + scaleOut(targetScale = 0.96f),
                        ) {
                            message?.let {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (animatedSuccess) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    },
                                ) {
                                    Text(
                                        it,
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                }
                            }
                        }
                        if (animatedSuccess) {
                            Button(
                                onClick = { index += 1 },
                                shape = CircleShape,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "આગળનું sentence")
                            }
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

private fun sentenceHint(expected: List<String>, selected: List<String>): String {
    val mismatch = selected.indices.firstOrNull { index ->
        index >= expected.size || !selected[index].equals(expected[index], ignoreCase = false)
    }
    if (mismatch == null && selected.size < expected.size) {
        return "હજુ ${expected.size - selected.size} word બાકી છે."
    }
    val expectedWord = expected.getOrNull(mismatch ?: 0).orEmpty()
    return when (expectedWord.lowercase().removeSuffix(".")) {
        "this" -> "નજીકની વસ્તુ માટે sentence ની શરૂઆત This થી કરો."
        "that" -> "દૂરની વસ્તુ માટે That વાપરો."
        "a" -> "વ્યંજનના અવાજ પહેલાં a આવે છે."
        "an" -> "a, e, i, o, u ના અવાજ પહેલાં an આવે છે."
        "is" -> "એક વસ્તુ માટે is વાપરો."
        "are" -> "એકથી વધુ વ્યક્તિ/વસ્તુ માટે are વાપરો."
        "and" -> "બે નામ અથવા બે ભાગ જોડવા and વાપરો."
        else -> "ક્રમ ફરી જુઓ. અહીં ‘$expectedWord’ આવવું જોઈએ."
    }
}

@Composable
private fun SelectedWordChips(words: List<String>, onRemove: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        words.chunked(3).forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { columnIndex, word ->
                    val absoluteIndex = rowIndex * 3 + columnIndex
                    Surface(
                        onClick = { onRemove(absoluteIndex) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                    ) {
                        Text(
                            word,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun WordChips(words: List<String>, onWord: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        words.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { word ->
                    OutlinedButton(
                        onClick = { onWord(word) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                    ) { Text(word, style = MaterialTheme.typography.titleSmall) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
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
