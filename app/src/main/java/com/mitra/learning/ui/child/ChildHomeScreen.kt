package com.mitra.learning.ui.child

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("🦁", style = MaterialTheme.typography.displayMedium)
                Column {
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
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        Text("શું કરીએ?", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall)

        ChildActionCard(
            emoji = "🎤",
            title = "મિત્રને પૂછો",
            subtitle = "તમારા textbook વિશે બોલીને કે લખીને સવાલ પૂછો",
            icon = Icons.Default.Chat,
            enabled = state.canPlay && !state.loading,
            onClick = onTalk,
        )
        ChildActionCard(
            emoji = "🎨",
            title = "રમતથી શીખો",
            subtitle = "રંગો • spelling • English sentences",
            icon = Icons.Default.AutoAwesome,
            enabled = state.canPlay && !state.loading,
            onClick = onActivities,
        )
        ChildActionCard(
            emoji = "🚀",
            title = "આજની શીખવાની રમત",
            subtitle = "પુસ્તક, riddles, missions અને પ્રશ્નો",
            icon = Icons.Default.PlayArrow,
            enabled = state.canPlay && !state.loading,
            onClick = onPlay,
        )
        ChildActionCard(
            emoji = "🧠",
            title = "કૌશલ્ય રમત",
            subtitle = "પહાડા • બે અંકના હિસાબ • જોડણી",
            icon = Icons.Default.School,
            enabled = state.canPlay && !state.loading,
            onClick = onSkills,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onBooks, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                Text("  પુસ્તકો")
            }
            OutlinedButton(onClick = onParent, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Text("  Parent")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChildActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(icon, contentDescription = null)
        }
    }
}
