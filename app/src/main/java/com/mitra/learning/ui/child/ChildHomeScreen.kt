package com.mitra.learning.ui.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChildHomeScreen(
    state: ChildHomeUiState,
    onPlay: () -> Unit,
    onSkills: () -> Unit,
    onBooks: () -> Unit,
    onParent: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🦁", style = MaterialTheme.typography.displayLarge)
        Text("મિત્ર", style = MaterialTheme.typography.displayMedium)
        Text("શીખો • રમો • શોધો", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        if (!state.loading) {
            Text(
                "આજે ${state.usedTodayMinutes} મિનિટ શીખ્યા • ${state.remainingTodayMinutes} મિનિટ બાકી",
                style = MaterialTheme.typography.bodyMedium,
            )
            state.messageGujarati?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onPlay,
            enabled = state.canPlay && !state.loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text("  રમીએ")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSkills,
            enabled = state.canPlay && !state.loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Icon(Icons.Default.School, contentDescription = null)
            Text("  કૌશલ્ય રમત • પહાડા • જોડણી")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBooks, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
            Text("  પુસ્તક")
        }
        Spacer(Modifier.height(28.dp))
        OutlinedButton(onClick = onParent, modifier = Modifier.heightIn(min = 52.dp)) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Text("  Parent")
        }
    }
}
