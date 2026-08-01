package com.mitra.learning.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Small, purposeful motion primitives for child-facing Mitra screens.
 *
 * These animations intentionally avoid variable rewards, autoplay loops that
 * block interaction, or high-frequency flashing. They are decorative and
 * always leave the learning task as the main focus.
 */
enum class MascotMood {
    IDLE,
    LISTENING,
    THINKING,
    CELEBRATING,
    ENCOURAGING,
}

@Composable
fun AnimatedMitraMascot(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
) {
    val motion = rememberInfiniteTransition(label = "mitra-mascot")
    val bob by motion.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == MascotMood.THINKING) -4f else -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mitra-bob",
    )
    val tilt by motion.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mitra-tilt",
    )
    val targetScale = when (mood) {
        MascotMood.CELEBRATING -> 1.12f
        MascotMood.LISTENING -> 1.04f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 420f),
        label = "mitra-mood-scale",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (mood == MascotMood.LISTENING) {
            ListeningRings(Modifier.fillMaxSize())
        }
        Surface(
            modifier = Modifier
                .size(size * 0.82f)
                .graphicsLayer {
                    translationY = bob
                    rotationZ = tilt
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 5.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = when (mood) {
                        MascotMood.THINKING -> "🤔"
                        MascotMood.CELEBRATING -> "🦁"
                        MascotMood.ENCOURAGING -> "🙂"
                        else -> "🦁"
                    },
                    style = MaterialTheme.typography.displayMedium,
                )
            }
        }
    }
}

@Composable
private fun ListeningRings(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.secondary
    val transition = rememberInfiniteTransition(label = "listening-rings")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1_250)),
        label = "listening-ring-phase",
    )
    Canvas(modifier) {
        repeat(2) { index ->
            val local = (phase + index * 0.5f) % 1f
            val radius = size.minDimension * (0.31f + local * 0.18f)
            drawCircle(
                color = color.copy(alpha = (1f - local) * 0.38f),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

@Composable
fun AnimatedLearningBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
    val tertiary = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f)
    val transition = rememberInfiniteTransition(label = "learning-background")
    val drift by transition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(5_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "background-drift",
    )

    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(primary, radius = size.minDimension * 0.13f, center = Offset(size.width * 0.12f, size.height * 0.18f + drift))
            drawCircle(secondary, radius = size.minDimension * 0.09f, center = Offset(size.width * 0.88f, size.height * 0.28f - drift))
            drawCircle(tertiary, radius = size.minDimension * 0.11f, center = Offset(size.width * 0.83f, size.height * 0.78f + drift * 0.6f))
            drawCircle(primary, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.18f, size.height * 0.88f - drift * 0.7f))
        }
        content()
    }
}

@Composable
fun AnimatedScale(
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 500f),
        label = "active-scale",
    )
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** A bounded one-shot celebration that restarts only when [trigger] changes. */
@Composable
fun SuccessBurst(
    trigger: Any?,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(1f) }
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFFF6C945),
    )
    LaunchedEffect(trigger) {
        if (trigger != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }
    Canvas(modifier.fillMaxSize()) {
        if (progress.value >= 1f) return@Canvas
        val p = progress.value
        val fade = 1f - p
        val radius = size.minDimension * (0.08f + p * 0.42f)
        repeat(12) { index ->
            val angle = (2.0 * PI * index / 12.0).toFloat()
            val point = Offset(
                x = center.x + cos(angle) * radius,
                y = center.y + sin(angle) * radius,
            )
            drawCircle(
                color = palette[index % palette.size].copy(alpha = fade),
                radius = (3.5f + (index % 3)) * density * fade.coerceAtLeast(0.25f),
                center = point,
            )
        }
    }
}

@Composable
fun ThinkingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking-dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900)),
        label = "thinking-phase",
    )
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(width = 54.dp, height = 20.dp)) {
        repeat(3) { index ->
            val local = ((phase * 3f) - index).coerceIn(0f, 1f)
            val y = center.y - sin(local * PI).toFloat() * 5.dp.toPx()
            drawCircle(
                color = color.copy(alpha = 0.45f + local * 0.55f),
                radius = 4.dp.toPx(),
                center = Offset(9.dp.toPx() + index * 18.dp.toPx(), y),
            )
        }
    }
}

fun Modifier.softPressScale(pressed: Boolean): Modifier = this.graphicsLayer {
    val value = if (pressed) 0.97f else 1f
    scaleX = value
    scaleY = value
}
