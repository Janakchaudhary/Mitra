package com.mitra.learning.ui.activity

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mitra.learning.ui.animation.AnimatedLearningBackground
import com.mitra.learning.ui.animation.AnimatedMitraMascot
import com.mitra.learning.ui.animation.MascotMood

@Composable
fun ActivityHubScreen(
    onColorLab: () -> Unit,
    onSentenceBuilder: () -> Unit,
    onBack: () -> Unit,
) {
    AnimatedLearningBackground(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                AnimatedMitraMascot(mood = MascotMood.IDLE, size = 58.dp)
                Column(Modifier.padding(start = 8.dp)) {
                    Text("રમતથી શીખીએ", style = MaterialTheme.typography.headlineMedium)
                    Text("જુઓ • કરો • બોલો • લખો")
                }
            }
            ActivityCard(
                emoji = "🎈",
                title = "રંગોની મજા",
                subtitle = "ચિત્ર રંગો → ગુજરાતી નામ → English → spelling",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                icon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                onClick = onColorLab,
            )
            ActivityCard(
                emoji = "🧩",
                title = "English Sentence Builder",
                subtitle = "this • that • and • is • are • a • an થી વાક્ય બનાવો",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                icon = { Icon(Icons.Default.Extension, contentDescription = null) },
                onClick = onSentenceBuilder,
            )
        }
    }
}

@Composable
private fun ActivityCard(
    emoji: String,
    title: String,
    subtitle: String,
    containerColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 520f),
        label = "activity-card-press",
    )
    Card(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 146.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pressed) 1.dp else 5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.displayMedium)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            icon()
        }
    }
}
