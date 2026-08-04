package com.mitra.learning.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ParentPinScreen(
    state: ParentPinUiState,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
    onDeviceUnlock: () -> Unit,
    deviceUnlockAvailable: Boolean,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Parent area", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.pin,
            onValueChange = onPinChange,
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(16.dp))
        Text("છેલ્લો PIN અંક દાખલ થતાં Parent area આપમેળે ખુલશે.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = onUnlock, enabled = !state.checking, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.checking) "Checking…" else "Unlock with parent PIN")
        }
        if (deviceUnlockAvailable) {
            OutlinedButton(onClick = onDeviceUnlock, enabled = !state.checking, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Text("  Fingerprint / phone lock")
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
fun ParentHomeScreen(
    onBooks: () -> Unit,
    onProgress: () -> Unit,
    onSettings: () -> Unit,
    onAiSettings: () -> Unit,
    onPractice: () -> Unit,
    onQuizBuilder: () -> Unit,
    onChildMode: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Parent dashboard", style = MaterialTheme.typography.headlineLarge)
        Text("Mitra 20 — voice tutor, 20/25-mark tests, visual lessons and offline books")
        Button(onClick = onBooks, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
            Text("  My books")
        }
        Button(onClick = onProgress, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ShowChart, contentDescription = null)
            Text("  Learning progress")
        }
        Button(onClick = onPractice, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PlayCircle, contentDescription = null)
            Text("  Choose a practice skill")
        }
        Button(onClick = onQuizBuilder, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Assignment, contentDescription = null)
            Text("  Build a 20/25-mark child test")
        }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Tune, contentDescription = null)
            Text("  Learning & privacy settings")
        }
        OutlinedButton(onClick = onAiSettings, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Text("  AI provider settings")
        }
        OutlinedButton(onClick = onChildMode, modifier = Modifier.fillMaxWidth()) {
            Text("Return to child mode")
        }
    }
}
