package com.mitra.learning.ui.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

private data class LearnColor(val gujarati: String, val english: String, val color: Color)

private val colors = listOf(
    LearnColor("લાલ", "red", Color(0xFFE94B4B)),
    LearnColor("વાદળી", "blue", Color(0xFF4D83E8)),
    LearnColor("લીલો", "green", Color(0xFF55A95C)),
    LearnColor("પીળો", "yellow", Color(0xFFF5C842)),
    LearnColor("નારંગી", "orange", Color(0xFFF28C38)),
)

private enum class ColorStep { PICK, GUJARATI, ENGLISH, SPELLING, DONE }

@Composable
fun ColorLabScreen(onBack: () -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    val target = colors[round % colors.size]
    var filled by remember(round) { mutableStateOf<Color?>(null) }
    var chosen by remember(round) { mutableStateOf<LearnColor?>(null) }
    var step by remember(round) { mutableStateOf(ColorStep.PICK) }
    var spelling by remember(round) { mutableStateOf("") }
    var message by remember(round) { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column {
                Text("🎨 રંગોની મજા", style = MaterialTheme.typography.headlineMedium)
                Text("રંગો અને ત્રણ ભાષા-કૌશલ્ય એક રમતમાં")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    when (step) {
                        ColorStep.PICK -> "પહેલા balloon ને કોઈ રંગથી ભરો!"
                        ColorStep.GUJARATI -> "તમે ભરેલો રંગ ગુજરાતીમાં શું કહેવાય?"
                        ColorStep.ENGLISH -> "Great! આ રંગનું English name શું છે?"
                        ColorStep.SPELLING -> "હવે English spelling લખો."
                        ColorStep.DONE -> "🌟 એક રંગ સંપૂર્ણ શીખી ગયા!"
                    },
                    style = MaterialTheme.typography.titleLarge,
                )
                BalloonDrawing(fill = filled)

                when (step) {
                    ColorStep.PICK -> ColorPalette { selected ->
                        chosen = selected
                        filled = selected.color
                        step = ColorStep.GUJARATI
                        message = null
                    }
                    ColorStep.GUJARATI -> {
                        val actual = chosen ?: target
                        ChoiceGrid(
                        options = colors.map { it.gujarati },
                        onChoice = { answer ->
                            if (answer == actual.gujarati) {
                                message = "સાચું! ${actual.gujarati} 👏"
                                step = ColorStep.ENGLISH
                            } else message = "ફરી જુઓ — ચિત્રના રંગ સાથે નામ મેળવો."
                        }
                        )
                    }
                    ColorStep.ENGLISH -> {
                        val actual = chosen ?: target
                        ChoiceGrid(
                        options = colors.map { it.english },
                        onChoice = { answer ->
                            if (answer.equals(actual.english, ignoreCase = true)) {
                                message = "Yes! ${actual.english}. હવે spelling!"
                                step = ColorStep.SPELLING
                            } else message = "Try again. રંગ જુઓ અને English name યાદ કરો."
                        }
                        )
                    }
                    ColorStep.SPELLING -> {
                        OutlinedTextField(
                            value = spelling,
                            onValueChange = { spelling = it.take(20) },
                            label = { Text("English spelling") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                val actual = chosen ?: target
                                if (spelling.trim().equals(actual.english, ignoreCase = true)) {
                                    message = "Perfect spelling: ${actual.english} ⭐"
                                    step = ColorStep.DONE
                                } else message = "લગભગ! અવાજ ધીમે બોલો અને ફરી spelling લખો."
                            },
                            enabled = spelling.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("ચેક કરો") }
                    }
                    ColorStep.DONE -> {
                        Button(
                            onClick = { round += 1 },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("બીજો રંગ રમીએ") }
                    }
                }
                message?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun BalloonDrawing(fill: Color?) {
    Canvas(modifier = Modifier.size(220.dp)) {
        val center = Offset(size.width / 2f, size.height * .40f)
        val radius = size.minDimension * .28f
        drawCircle(color = fill ?: Color.White, radius = radius, center = center)
        drawCircle(color = Color(0xFF303030), radius = radius, center = center, style = Stroke(width = 7f))
        val knotTop = center.y + radius
        drawLine(Color(0xFF303030), Offset(center.x, knotTop), Offset(center.x - 8f, knotTop + 18f), strokeWidth = 6f)
        drawLine(Color(0xFF303030), Offset(center.x, knotTop), Offset(center.x + 8f, knotTop + 18f), strokeWidth = 6f)
        drawLine(Color(0xFF6B6B6B), Offset(center.x, knotTop + 18f), Offset(center.x + 22f, size.height - 4f), strokeWidth = 4f)
    }
}

@Composable
private fun ColorPalette(onSelected: (LearnColor) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(item.color, CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .clickable { onSelected(item) },
                    )
                }
            }
        }
        Text("રંગ પસંદ કરીને balloon ભરો", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ChoiceGrid(options: List<String>, onChoice: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.distinct().chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { option ->
                    OutlinedButton(onClick = { onChoice(option) }, modifier = Modifier.weight(1f)) { Text(option) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
