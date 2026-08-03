package com.mitra.learning.ui.child

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.MascotMood

@Composable
fun ChildHomeScreen(
    state: ChildHomeUiState,
    onPlay: () -> Unit,
    onSkills: () -> Unit,
    onTalk: () -> Unit,
    onActivities: () -> Unit,
    onBooks: () -> Unit,
    onParent: () -> Unit,
) {
    AnimatedLearningBackground(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnimatedMitraMascot(
                        mood = if (state.loading) MascotMood.THINKING else MascotMood.IDLE,
                        size = 82.dp,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("મિત્ર", style = MaterialTheme.typography.displaySmall)
                        Text("આજે કંઈક નવું શોધીએ! ✨", style = MaterialTheme.typography.titleMedium)
                        if (!state.loading) {
                            Text(
                                "${state.usedTodayMinutes} મિનિટ શીખ્યા • ${state.remainingTodayMinutes} મિનિટ બાકી",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            state.messageGujarati?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(it, modifier = Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("શું કરીએ?", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall)

            ChildActionCard(
                emoji = "🎤",
                title = "મિત્રને પૂછો",
                subtitle = "પુસ્તક પૂછો • ઘડિયા • પહેલાં-પછી • voice spelling",
                icon = Icons.Default.Chat,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                enabled = state.canPlay && !state.loading,
                onClick = onTalk,
            )
            ChildActionCard(
                emoji = "🎨",
                title = "રમતથી શીખો",
                subtitle = "રંગો • spelling • English sentences",
                icon = Icons.Default.AutoAwesome,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                enabled = state.canPlay && !state.loading,
                onClick = onActivities,
            )
            ChildActionCard(
                emoji = "🚀",
                title = "આજની શીખવાની રમત",
                subtitle = "પુસ્તક, riddles, missions અને પ્રશ્નો",
                icon = Icons.Default.PlayArrow,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                enabled = state.canPlay && !state.loading,
                onClick = onPlay,
            )
            ChildActionCard(
                emoji = "🧠",
                title = "કૌશલ્ય રમત",
                subtitle = "બે અંક • કેરિ ચેલેન્જ • રફ કામ • પહાડા",
                icon = Icons.Default.School,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                enabled = state.canPlay && !state.loading,
                onClick = onSkills,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onBooks, modifier = Modifier.weight(1f).heightIn(min = 58.dp), shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                    Text("  પુસ્તકો")
                }
                OutlinedButton(onClick = onParent, modifier = Modifier.weight(1f).heightIn(min = 58.dp), shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Text("  Parent")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChildActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "home-card-press",
    )
    val floatTransition = rememberInfiniteTransition(label = "home-card-emoji")
    val emojiOffset by floatTransition.animateFloat(
        initialValue = -2f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "home-card-emoji-offset",
    )

    Card(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 116.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pressed) 1.dp else 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                emoji,
                modifier = Modifier.graphicsLayer { translationY = emojiOffset },
                style = MaterialTheme.typography.displaySmall,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(icon, contentDescription = null)
        }
    }
}
