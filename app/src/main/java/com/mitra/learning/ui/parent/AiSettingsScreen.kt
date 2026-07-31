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
import androidx.compose.material3.FilterChip
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
import com.mitra.learning.ai.settings.AiProviderConfig
import com.mitra.learning.ai.settings.AiProviderType

@Composable
fun AiSettingsScreen(
    state: AiSettingsUiState,
    onRemoteEnabledChange: (Boolean) -> Unit,
    onProviderChange: (AiProviderType) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onCloudflareAccountIdChange: (String) -> Unit,
    onCredentialChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onClearCredential: () -> Unit,
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
            "OpenAI remains the default. Cloudflare Workers AI is available as a second provider with a free daily allocation. Built-in skill practice remains local/offline.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "When remote AI is enabled, textbook pages/context and Study Talk questions can be sent to the selected provider. Raw microphone audio is not sent by Mitra.",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Use remote AI for textbook features")
                Text(
                    if (state.remoteEnabled) "Remote provider enabled" else "Offline/mock AI enabled",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = state.remoteEnabled, onCheckedChange = onRemoteEnabledChange)
        }

        Text("Provider", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.provider == AiProviderType.OPENAI,
                onClick = { onProviderChange(AiProviderType.OPENAI) },
                label = { Text("OpenAI") },
                enabled = state.remoteEnabled,
            )
            FilterChip(
                selected = state.provider == AiProviderType.CLOUDFLARE,
                onClick = { onProviderChange(AiProviderType.CLOUDFLARE) },
                label = { Text("Cloudflare Free") },
                enabled = state.remoteEnabled,
            )
        }

        if (state.provider == AiProviderType.OPENAI) {
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text("OpenAI API base URL") },
                enabled = state.remoteEnabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.model,
                onValueChange = onModelChange,
                label = { Text("OpenAI model") },
                enabled = state.remoteEnabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.credentialDraft,
                onValueChange = onCredentialChange,
                label = { Text(if (state.hasStoredCredential) "Replace saved OpenAI key" else "OpenAI API key") },
                placeholder = { Text(if (state.hasStoredCredential) "A key is already saved" else "sk-…") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = state.remoteEnabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                "Cloudflare setup: create a free Cloudflare account, open Workers AI → Use REST API, then copy the Account ID and Workers AI API token.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = state.cloudflareAccountId,
                onValueChange = onCloudflareAccountIdChange,
                label = { Text("Cloudflare Account ID") },
                placeholder = { Text("32-character account ID") },
                enabled = state.remoteEnabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.model,
                onValueChange = onModelChange,
                label = { Text("Workers AI model") },
                supportingText = { Text("Default: ${AiProviderConfig.DEFAULT_CLOUDFLARE_MODEL}") },
                enabled = state.remoteEnabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.credentialDraft,
                onValueChange = onCredentialChange,
                label = { Text(if (state.hasStoredCredential) "Replace saved Cloudflare token" else "Workers AI API token") },
                placeholder = { Text(if (state.hasStoredCredential) "A token is already saved" else "Paste API token") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = state.remoteEnabled,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Only Cloudflare-hosted @cf/... models are accepted in this mode. Free-tier quotas are controlled by Cloudflare and can change.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (state.hasStoredCredential) {
            OutlinedButton(onClick = onClearCredential, modifier = Modifier.fillMaxWidth()) {
                Text("Remove saved ${if (state.provider == AiProviderType.OPENAI) "OpenAI key" else "Cloudflare token"}")
            }
        }

        Text(
            "Personal/development mode: credentials are entered after installation and encrypted with an Android Keystore-protected key. Credentials in a mobile app can still be extracted from a compromised device; do not publish a configured APK.",
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
