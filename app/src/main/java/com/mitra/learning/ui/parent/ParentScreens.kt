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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
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
        Button(onClick = onUnlock, enabled = !state.checking, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.checking) "Checking…" else "Unlock")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
fun ParentHomeScreen(
    onBooks: () -> Unit,
    onProgress: () -> Unit,
    onAiSettings: () -> Unit,
    onChildMode: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Parent dashboard", style = MaterialTheme.typography.headlineLarge)
        Text("Milestone 7 — local progress dashboard + adaptive review")
        Button(onClick = onBooks, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.MenuBook, contentDescription = null)
            Text("  My books")
        }
        Button(onClick = onProgress, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ShowChart, contentDescription = null)
            Text("  Learning progress")
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
