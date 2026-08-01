package com.mitra.learning.ui.activity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.MascotMood
import com.mitra.learning.ui.animation.SuccessBurst
import kotlin.random.Random

private data class LearnColor(
    val gujarati: String,
    val english: String,
    val color: Color,
)

private enum class PictureType { BALLOON, APPLE, KITE, FISH, FLOWER, CAR }

private data class ColorPicture(
    val gujarati: String,
    val english: String,
    val type: PictureType,
)

private data class ColorRound(
    val picture: ColorPicture,
    val target: LearnColor,
)

private val colors = listOf(
    LearnColor("લાલ", "red", Color(0xFFE94B4B)),
    LearnColor("વાદળી", "blue", Color(0xFF4D83E8)),
    LearnColor("લીલો", "green", Color(0xFF55A95C)),
    LearnColor("પીળો", "yellow", Color(0xFFF5C842)),
    LearnColor("નારંગી", "orange", Color(0xFFF28C38)),
    LearnColor("જાંબલી", "purple", Color(0xFF8E62C7)),
    LearnColor("ગુલાબી", "pink", Color(0xFFF27AA8)),
    LearnColor("ભૂરો", "brown", Color(0xFF9A6A45)),
)

private val pictures = listOf(
    ColorPicture("ફુગ્ગો", "balloon", PictureType.BALLOON),
    ColorPicture("સફરજન", "apple", PictureType.APPLE),
    ColorPicture("પતંગ", "kite", PictureType.KITE),
    ColorPicture("માછલી", "fish", PictureType.FISH),
    ColorPicture("ફૂલ", "flower", PictureType.FLOWER),
    ColorPicture("ગાડી", "car", PictureType.CAR),
)

private enum class ColorStep { PICK, GUJARATI, ENGLISH, SPELLING, DONE }

@Composable
fun ColorLabScreen(onBack: () -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    val currentRound = remember(round) { createColorRound(round) }
    var filled by remember(round) { mutableStateOf<Color?>(null) }
    var step by remember(round) { mutableStateOf(ColorStep.PICK) }
    var spelling by remember(round) { mutableStateOf("") }
    var message by remember(round) { mutableStateOf<String?>(null) }

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
                    mood = if (step == ColorStep.DONE) MascotMood.CELEBRATING else MascotMood.IDLE,
                    size = 50.dp,
                )
                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                    Text("રંગોની રમત", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("ચિત્ર ભરો • ગુજરાતી • English • spelling", style = MaterialTheme.typography.bodySmall)
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        "${round % pictures.size + 1}/${pictures.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (step.ordinal + 1).toFloat() / ColorStep.entries.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.94f)) togetherWith
                                (fadeOut() + scaleOut(targetScale = 0.96f))
                        },
                        label = "color-step-title",
                    ) { shownStep ->
                        Text(
                            when (shownStep) {
                                ColorStep.PICK -> "${currentRound.picture.gujarati}ને બતાવેલા રંગથી ભરો."
                                ColorStep.GUJARATI -> "આ રંગ ગુજરાતીમાં કયો છે?"
                                ColorStep.ENGLISH -> "આ રંગનું English name શું છે?"
                                ColorStep.SPELLING -> "હવે ${currentRound.target.english} ની spelling લખો."
                                ColorStep.DONE -> "🌟 ${currentRound.picture.gujarati} અને રંગ બંને શીખ્યા!"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    if (step == ColorStep.PICK) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("રંગનો નમૂનો:", style = MaterialTheme.typography.labelLarge)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(currentRound.target.color, CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            )
                        }
                    }
                    PictureDrawing(
                        picture = currentRound.picture,
                        fill = filled,
                    )
                    Text(
                        "${currentRound.picture.gujarati} • ${currentRound.picture.english}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith fadeOut() },
                        label = "color-step-controls",
                    ) { shownStep ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            when (shownStep) {
                                ColorStep.PICK -> ColorPalette { selected ->
                                    if (selected == currentRound.target) {
                                        filled = selected.color
                                        message = "સાચો રંગ પસંદ કર્યો! 👏"
                                        step = ColorStep.GUJARATI
                                    } else {
                                        message = "ફરી જુઓ — ઉપરના નમૂના જેવો રંગ શોધો."
                                    }
                                }

                                ColorStep.GUJARATI -> ChoiceGrid(
                                    options = colorOptions(currentRound.target, gujarati = true, seed = round + 11),
                                    onChoice = { answer ->
                                        if (answer == currentRound.target.gujarati) {
                                            message = "સાચું! ${currentRound.target.gujarati} 👏"
                                            step = ColorStep.ENGLISH
                                        } else {
                                            message = "ચિત્રનો રંગ જુઓ અને ફરી પસંદ કરો."
                                        }
                                    },
                                )

                                ColorStep.ENGLISH -> ChoiceGrid(
                                    options = colorOptions(currentRound.target, gujarati = false, seed = round + 31),
                                    onChoice = { answer ->
                                        if (answer.equals(currentRound.target.english, ignoreCase = true)) {
                                            message = "Yes! ${currentRound.target.english}. હવે spelling!"
                                            step = ColorStep.SPELLING
                                        } else {
                                            message = "Try again. રંગનું English name યાદ કરો."
                                        }
                                    },
                                )

                                ColorStep.SPELLING -> {
                                    OutlinedTextField(
                                        value = spelling,
                                        onValueChange = { spelling = it.take(20) },
                                        label = { Text("English spelling") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                    )
                                    Button(
                                        onClick = {
                                            if (spelling.trim().equals(currentRound.target.english, ignoreCase = true)) {
                                                message = "Perfect spelling: ${currentRound.target.english} ⭐"
                                                step = ColorStep.DONE
                                            } else {
                                                message = "લગભગ! શબ્દ ધીમે બોલો અને ફરી લખો."
                                            }
                                        },
                                        enabled = spelling.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                    ) { Text("ચેક કરો") }
                                }

                                ColorStep.DONE -> Button(
                                    onClick = { round += 1 },
                                    shape = CircleShape,
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "આગળનું ચિત્ર")
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = message != null,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f),
                        exit = fadeOut(),
                    ) {
                        message?.let {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    it,
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
        SuccessBurst(
            trigger = if (step == ColorStep.DONE) "color-$round" else null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun createColorRound(index: Int): ColorRound {
    val picture = pictures[index % pictures.size]
    val target = colors[(index * 3 + index / pictures.size + 1) % colors.size]
    return ColorRound(picture = picture, target = target)
}

private fun colorOptions(target: LearnColor, gujarati: Boolean, seed: Int): List<String> {
    val random = Random(seed)
    val choices = colors
        .filterNot { it == target }
        .shuffled(random)
        .take(3) + target
    return choices.shuffled(random).map { if (gujarati) it.gujarati else it.english }
}

@Composable
private fun PictureDrawing(
    picture: ColorPicture,
    fill: Color?,
) {
    val animatedFill by animateColorAsState(
        targetValue = fill ?: Color.White,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "picture-fill",
    )
    val motion = rememberInfiniteTransition(label = "picture-motion")
    val offset by motion.animateFloat(
        initialValue = -3f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "picture-offset",
    )
    Canvas(
        modifier = Modifier
            .size(190.dp)
            .graphicsLayer { translationY = offset },
    ) {
        drawColorPicture(
            type = picture.type,
            fill = animatedFill,
            outline = Color(0xFF303030),
        )
    }
}

private fun DrawScope.drawColorPicture(type: PictureType, fill: Color, outline: Color) {
    val stroke = 6f
    when (type) {
        PictureType.BALLOON -> {
            val center = Offset(size.width / 2f, size.height * .40f)
            val radius = size.minDimension * .28f
            drawCircle(fill, radius, center)
            drawCircle(outline, radius, center, style = Stroke(stroke))
            drawCircle(Color.White.copy(alpha = .35f), radius * .22f, Offset(center.x - radius * .32f, center.y - radius * .34f))
            val knotTop = center.y + radius
            drawLine(outline, Offset(center.x, knotTop), Offset(center.x - 8f, knotTop + 18f), strokeWidth = stroke)
            drawLine(outline, Offset(center.x, knotTop), Offset(center.x + 8f, knotTop + 18f), strokeWidth = stroke)
            drawLine(Color(0xFF6B6B6B), Offset(center.x, knotTop + 18f), Offset(center.x + 18f, size.height - 5f), strokeWidth = 4f)
        }

        PictureType.APPLE -> {
            drawOval(
                color = fill,
                topLeft = Offset(size.width * .22f, size.height * .25f),
                size = Size(size.width * .56f, size.height * .52f),
            )
            drawOval(
                color = outline,
                topLeft = Offset(size.width * .22f, size.height * .25f),
                size = Size(size.width * .56f, size.height * .52f),
                style = Stroke(stroke),
            )
            drawLine(outline, Offset(size.width * .52f, size.height * .27f), Offset(size.width * .55f, size.height * .14f), strokeWidth = 8f)
            drawOval(
                color = Color(0xFF55A95C),
                topLeft = Offset(size.width * .53f, size.height * .12f),
                size = Size(size.width * .19f, size.height * .10f),
            )
        }

        PictureType.KITE -> {
            val path = Path().apply {
                moveTo(size.width * .50f, size.height * .12f)
                lineTo(size.width * .78f, size.height * .43f)
                lineTo(size.width * .50f, size.height * .72f)
                lineTo(size.width * .22f, size.height * .43f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, outline, style = Stroke(stroke))
            drawLine(outline, Offset(size.width * .50f, size.height * .12f), Offset(size.width * .50f, size.height * .72f), strokeWidth = 4f)
            drawLine(outline, Offset(size.width * .22f, size.height * .43f), Offset(size.width * .78f, size.height * .43f), strokeWidth = 4f)
            drawLine(outline, Offset(size.width * .50f, size.height * .72f), Offset(size.width * .60f, size.height * .94f), strokeWidth = 4f)
        }

        PictureType.FISH -> {
            drawOval(
                color = fill,
                topLeft = Offset(size.width * .20f, size.height * .30f),
                size = Size(size.width * .56f, size.height * .36f),
            )
            drawOval(
                color = outline,
                topLeft = Offset(size.width * .20f, size.height * .30f),
                size = Size(size.width * .56f, size.height * .36f),
                style = Stroke(stroke),
            )
            val tail = Path().apply {
                moveTo(size.width * .22f, size.height * .48f)
                lineTo(size.width * .05f, size.height * .28f)
                lineTo(size.width * .05f, size.height * .68f)
                close()
            }
            drawPath(tail, fill)
            drawPath(tail, outline, style = Stroke(stroke))
            drawCircle(outline, radius = 7f, center = Offset(size.width * .65f, size.height * .42f))
        }

        PictureType.FLOWER -> {
            val center = Offset(size.width * .50f, size.height * .43f)
            val petalRadius = size.width * .12f
            listOf(
                Offset(center.x, center.y - petalRadius * 1.45f),
                Offset(center.x + petalRadius * 1.45f, center.y),
                Offset(center.x, center.y + petalRadius * 1.45f),
                Offset(center.x - petalRadius * 1.45f, center.y),
            ).forEach { petal ->
                drawCircle(fill, petalRadius, petal)
                drawCircle(outline, petalRadius, petal, style = Stroke(stroke))
            }
            drawCircle(Color(0xFFF5C842), petalRadius, center)
            drawCircle(outline, petalRadius, center, style = Stroke(stroke))
            drawLine(Color(0xFF55A95C), Offset(center.x, center.y + petalRadius), Offset(center.x, size.height * .92f), strokeWidth = 9f)
        }

        PictureType.CAR -> {
            drawRoundRect(
                color = fill,
                topLeft = Offset(size.width * .13f, size.height * .42f),
                size = Size(size.width * .74f, size.height * .30f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
            )
            drawRoundRect(
                color = outline,
                topLeft = Offset(size.width * .13f, size.height * .42f),
                size = Size(size.width * .74f, size.height * .30f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
                style = Stroke(stroke),
            )
            val roof = Path().apply {
                moveTo(size.width * .32f, size.height * .42f)
                lineTo(size.width * .43f, size.height * .25f)
                lineTo(size.width * .68f, size.height * .25f)
                lineTo(size.width * .77f, size.height * .42f)
                close()
            }
            drawPath(roof, fill)
            drawPath(roof, outline, style = Stroke(stroke))
            drawCircle(outline, radius = size.width * .085f, center = Offset(size.width * .32f, size.height * .74f))
            drawCircle(outline, radius = size.width * .085f, center = Offset(size.width * .70f, size.height * .74f))
            drawCircle(Color.White, radius = size.width * .035f, center = Offset(size.width * .32f, size.height * .74f))
            drawCircle(Color.White, radius = size.width * .035f, center = Offset(size.width * .70f, size.height * .74f))
        }
    }
}

@Composable
private fun ColorPalette(onSelected: (LearnColor) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { item ->
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(item.color, CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .clickable { onSelected(item) },
                    )
                }
            }
        }
        Text("સૂચના પ્રમાણે સાચો રંગ પસંદ કરો", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ChoiceGrid(options: List<String>, onChoice: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        options.distinct().chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                pair.forEach { option ->
                    OutlinedButton(
                        onClick = { onChoice(option) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(option) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
