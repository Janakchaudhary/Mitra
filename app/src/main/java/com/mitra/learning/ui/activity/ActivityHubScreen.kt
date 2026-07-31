package com.mitra.learning.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActivityHubScreen(
    onColorLab: () -> Unit,
    onSentenceBuilder: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column {
                Text("🎨 રમતથી શીખીએ", style = MaterialTheme.typography.headlineMedium)
                Text("જુઓ • કરો • બોલો • લખો")
            }
        }
        ActivityCard(
            emoji = "🎈",
            title = "રંગોની મજા",
            subtitle = "ચિત્ર રંગો → ગુજરાતી નામ → English → spelling",
            icon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
            onClick = onColorLab,
        )
        ActivityCard(
            emoji = "🧩",
            title = "English Sentence Builder",
            subtitle = "this • that • and • is • are • a • an થી વાક્ય બનાવો",
            icon = { Icon(Icons.Default.Extension, contentDescription = null) },
            onClick = onSentenceBuilder,
        )
    }
}

@Composable
private fun ActivityCard(
    emoji: String,
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            icon()
        }
    }
}
