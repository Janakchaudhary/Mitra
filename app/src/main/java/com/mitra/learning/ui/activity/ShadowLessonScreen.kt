package com.mitra.learning.ui.activity

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.MascotMood
import com.mitra.learning.voice.SpeechOutput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class ShadowStep(
    val title: String,
    val explanation: String,
    val sunX: Float,
    val sunY: Float,
    val shadowDirection: Float,
    val shadowLength: Float,
)

private val shadowSteps = listOf(
    ShadowStep("પડછાયો શું છે?", "પ્રકાશને કોઈ વસ્તુ રોકે ત્યારે તેની પાછળ અંધારું આકાર બને છે. તેને પડછાયો કહે છે.", .18f, .22f, 1f, .34f),
    ShadowStep("સૂર્ય સામે પડછાયો", "પડછાયો હંમેશાં પ્રકાશની વિરુદ્ધ બાજુ બને છે. સૂર્ય ડાબે હોય તો પડછાયો જમણે પડે.", .16f, .26f, 1f, .42f),
    ShadowStep("સવારે લાંબો", "સવારે સૂર્ય નીચો હોય છે, તેથી પડછાયો લાંબો બને છે.", .12f, .42f, 1f, .52f),
    ShadowStep("બપોરે નાનો", "બપોરે સૂર્ય માથા ઉપર હોય છે, તેથી પડછાયો સૌથી નાનો બને છે.", .50f, .12f, .15f, .15f),
    ShadowStep("સાંજે દિશા બદલાય", "સાંજે સૂર્ય જમણે હોય છે. હવે પડછાયો ડાબી બાજુ લાંબો પડે છે.", .88f, .42f, -1f, .52f),
    ShadowStep("પ્રયોગ કરો", "ટોર્ચ અને પેન્સિલ લો. ટોર્ચને અલગ અલગ બાજુ ખસેડો અને પડછાયાની દિશા તથા લંબાઈ જુઓ.", .28f, .28f, 1f, .38f),
)

@Composable
fun ShadowLessonScreen(
    speechOutput: SpeechOutput,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var stepIndex by remember { mutableIntStateOf(0) }
    var autoPlaying by remember { mutableStateOf(false) }
    val step = shadowSteps[stepIndex]
    val sunX by animateFloatAsState(step.sunX, tween(900), label = "sun-x")
    val sunY by animateFloatAsState(step.sunY, tween(900), label = "sun-y")
    val direction by animateFloatAsState(step.shadowDirection, tween(900), label = "shadow-direction")
    val length by animateFloatAsState(step.shadowLength, tween(900), label = "shadow-length")

    LaunchedEffect(stepIndex) {
        speechOutput.speakGujarati("${step.title}. ${step.explanation}")
    }
    DisposableEffect(Unit) { onDispose { speechOutput.stop() } }

    AnimatedLearningBackground(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "પાછા") }
                AnimatedMitraMascot(mood = MascotMood.ENCOURAGING, size = 52.dp)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("દૃશ્યથી શીખીએ — પડછાયો", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("જુઓ • સાંભળો • આગળ વધો", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { scope.launch { speechOutput.speakGujarati("${step.title}. ${step.explanation}") } }) {
                    Icon(Icons.Default.VolumeUp, "ફરી સાંભળો")
                }
            }
            LinearProgressIndicator(progress = { (stepIndex + 1f) / shadowSteps.size }, modifier = Modifier.fillMaxWidth())

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(step.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFFE9F6FF), RoundedCornerShape(18.dp))) {
                        Canvas(Modifier.fillMaxSize()) {
                            val groundY = size.height * .76f
                            drawRect(Color(0xFFB9DD8B), topLeft = Offset(0f, groundY), size = Size(size.width, size.height - groundY))

                            val sx = size.width * sunX
                            val sy = size.height * sunY
                            drawCircle(Color(0xFFFFD54F), radius = size.minDimension * .075f, center = Offset(sx, sy))
                            repeat(12) { ray ->
                                val angle = ray * Math.PI * 2 / 12
                                val inner = size.minDimension * .095f
                                val outer = size.minDimension * .13f
                                drawLine(
                                    Color(0xFFFFB300),
                                    Offset(sx + kotlin.math.cos(angle).toFloat() * inner, sy + kotlin.math.sin(angle).toFloat() * inner),
                                    Offset(sx + kotlin.math.cos(angle).toFloat() * outer, sy + kotlin.math.sin(angle).toFloat() * outer),
                                    strokeWidth = 4.dp.toPx(),
                                )
                            }

                            val objectX = size.width * .5f
                            val objectBase = groundY
                            val objectTop = groundY - size.height * .28f
                            drawRect(Color(0xFF795548), Offset(objectX - 11.dp.toPx(), objectTop), Size(22.dp.toPx(), objectBase - objectTop))
                            drawCircle(Color(0xFF4CAF50), size.minDimension * .09f, Offset(objectX, objectTop))

                            val shadowEndX = objectX + size.width * length * direction
                            val shadowWidth = size.minDimension * (.045f + .035f * abs(direction))
                            val shadowPath = Path().apply {
                                moveTo(objectX - shadowWidth, groundY + 4.dp.toPx())
                                lineTo(shadowEndX, groundY + shadowWidth * .45f)
                                lineTo(objectX + shadowWidth, groundY + 4.dp.toPx())
                                close()
                            }
                            drawPath(shadowPath, Color.Black.copy(alpha = .34f))
                            drawLine(Color(0xFFFFA000).copy(alpha = .55f), Offset(sx, sy), Offset(objectX, objectTop), strokeWidth = 2.dp.toPx(), pathEffect = null)
                        }
                    }
                    Text(step.explanation, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                    if (stepIndex == shadowSteps.lastIndex) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Text("સુરક્ષા: ટોર્ચ વાપરો; સૂર્ય તરફ સીધું ન જુઓ.", Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { stepIndex = (stepIndex - 1).coerceAtLeast(0) },
                    enabled = stepIndex > 0 && !autoPlaying,
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Text(" પાછળ") }
                FilledTonalButton(
                    onClick = {
                        if (!autoPlaying) scope.launch {
                            autoPlaying = true
                            try {
                                for (index in stepIndex..shadowSteps.lastIndex) {
                                    stepIndex = index
                                    if (index < shadowSteps.lastIndex) delay(3_800)
                                }
                            } finally {
                                autoPlaying = false
                            }
                        }
                    },
                    enabled = !autoPlaying,
                    modifier = Modifier.weight(1f),
                ) { Icon(Icons.Default.PlayArrow, null); Text(if (autoPlaying) " ચાલે છે…" else " Auto") }
                Button(
                    onClick = { if (stepIndex < shadowSteps.lastIndex) stepIndex += 1 else stepIndex = 0 },
                    enabled = !autoPlaying,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(if (stepIndex < shadowSteps.lastIndex) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Replay, null)
                    Text(if (stepIndex < shadowSteps.lastIndex) " આગળ" else " ફરી")
                }
            }
        }
    }
}
