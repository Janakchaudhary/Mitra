package com.mitra.learning.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AiSettingsScreen(
    state: AiSettingsUiState,
    onRemoteEnabledChange: (Boolean) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onClearKey: () -> Unit,
    onBack: () -> Unit,
) {
    if (state.loading) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("AI provider", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Parent-only setup. Child voice transcripts and answers are not sent to the remote AI in Milestone 5.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Use OpenAI for textbook analysis")
                Text(
                    if (state.remoteEnabled) "Remote AI enabled" else "Mock/offline AI enabled",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = state.remoteEnabled, onCheckedChange = onRemoteEnabledChange)
        }

        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("API base URL") },
            enabled = state.remoteEnabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.model,
            onValueChange = onModelChange,
            label = { Text("Model") },
            enabled = state.remoteEnabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.apiKeyDraft,
            onValueChange = onApiKeyChange,
            label = { Text(if (state.hasStoredApiKey) "Replace saved API key" else "API key") },
            placeholder = { Text(if (state.hasStoredApiKey) "A key is already saved" else "sk-…") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = state.remoteEnabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.hasStoredApiKey) {
            OutlinedButton(onClick = onClearKey, modifier = Modifier.fillMaxWidth()) {
                Text("Remove saved API key")
            }
        }

        Text(
            "Personal/development mode only: the key is entered after installation and encrypted with an Android Keystore-protected key. A mobile API key can still be extracted from a compromised device; do not publish this configuration.",
            style = MaterialTheme.typography.bodySmall,
        )

        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = onSave,
            enabled = !state.saving && !state.testing,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.saving) "Saving…" else "Save") }

        OutlinedButton(
            onClick = onTest,
            enabled = state.remoteEnabled && !state.saving && !state.testing,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.testing) "Testing…" else "Test connection") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
