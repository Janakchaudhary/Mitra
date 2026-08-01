package com.mitra.learning.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun PracticePickerScreen(
    state: PracticePickerUiState,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("ચોક્કસ કૌશલ્ય પસંદ કરો", style = MaterialTheme.typography.headlineSmall)
                    Text("Parent-selected 6-question practice", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (state.loading) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        state.choices.groupBy { it.subject }.forEach { (subject, choices) ->
            item { Text(subject, style = MaterialTheme.typography.titleLarge) }
            items(choices, key = { it.conceptId }) { choice ->
                Card(onClick = { onSelect(choice.conceptId) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(choice.titleGujarati, style = MaterialTheme.typography.titleMedium)
                            Text("${(choice.mastery.coerceIn(0f, 1f) * 100).roundToInt()}%")
                        }
                        LinearProgressIndicator(
                            progress = { choice.mastery.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            if (choice.fromBook) "📖 Prepared textbook" else "🧠 Built-in Standard 2 skill",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
