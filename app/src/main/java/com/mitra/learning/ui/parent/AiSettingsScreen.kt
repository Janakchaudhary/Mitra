package com.mitra.learning.ui.parent

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    onImportLocalModel: (android.net.Uri) -> Unit,
    onRemoveLocalModel: () -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onClearCredential: () -> Unit,
    onBack: () -> Unit,
) {
    val localModelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportLocalModel)
    }

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
            "OpenAI is the default. Cloudflare is the free cloud option. Offline Local uses prepared textbook text entirely on this phone and can optionally use a parent-imported LiteRT-LM model.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Provider", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = state.provider == AiProviderType.OPENAI,
                onClick = { onProviderChange(AiProviderType.OPENAI) },
                label = { Text("OpenAI") },
            )
            FilterChip(
                selected = state.provider == AiProviderType.CLOUDFLARE,
                onClick = { onProviderChange(AiProviderType.CLOUDFLARE) },
                label = { Text("Cloudflare") },
            )
            FilterChip(
                selected = state.provider == AiProviderType.OFFLINE_LOCAL,
                onClick = { onProviderChange(AiProviderType.OFFLINE_LOCAL) },
                label = { Text("Offline") },
            )
        }

        if (!state.isOfflineLocal) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Use cloud AI for textbook features")
                    Text(
                        if (state.remoteEnabled) "Cloud provider enabled" else "Cloud provider disabled",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = state.remoteEnabled, onCheckedChange = onRemoteEnabledChange)
            }
            Text(
                "Prepared page context and Study Talk questions may be sent to the selected cloud provider. Raw microphone audio is not sent by Mitra.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        when (state.provider) {
            AiProviderType.OPENAI -> {
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
                CredentialField(
                    state = state,
                    label = if (state.hasStoredCredential) "Replace saved OpenAI key" else "OpenAI API key",
                    placeholder = if (state.hasStoredCredential) "A key is already saved" else "sk-…",
                    onCredentialChange = onCredentialChange,
                )
            }

            AiProviderType.CLOUDFLARE -> {
                Text(
                    "Create a Cloudflare account, open Workers AI → Use REST API, then copy the Account ID and Workers AI token.",
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
                CredentialField(
                    state = state,
                    label = if (state.hasStoredCredential) "Replace saved Cloudflare token" else "Workers AI API token",
                    placeholder = if (state.hasStoredCredential) "A token is already saved" else "Paste API token",
                    onCredentialChange = onCredentialChange,
                )
                Text(
                    "Only Cloudflare-hosted @cf/... models are accepted. If Cloudflare changes a response format, Mitra now falls back to local prepared-book extraction instead of showing the child a parser error.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            AiProviderType.OFFLINE_LOCAL -> {
                Text(
                    "Offline Local answers maths locally and searches chapters that were already prepared. It never sends the child's question or textbook content to the internet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Without a model, Mitra uses grounded sentence extraction. For more natural conversation, import a compatible .litertlm model. Useful models are usually 0.5–3 GB and work best on a modern phone with at least 6–8 GB RAM.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = { localModelPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    enabled = !state.importingModel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.importingModel) "Importing local model…" else if (state.hasLocalModel) "Replace local .litertlm model" else "Import local .litertlm model")
                }
                if (state.hasLocalModel) {
                    Text("Installed local model: ${state.localModelSize ?: "ready"}", color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(onClick = onRemoveLocalModel, modifier = Modifier.fillMaxWidth()) {
                        Text("Remove local model")
                    }
                }
                Text(
                    "New scanned PDF pages still need OpenAI/Cloudflare once for preparation, or chapter ranges can be entered manually. After preparation, textbook chat and saved question banks work offline.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            AiProviderType.MOCK -> Unit
        }

        if (state.hasStoredCredential) {
            OutlinedButton(onClick = onClearCredential, modifier = Modifier.fillMaxWidth()) {
                Text("Remove saved ${if (state.provider == AiProviderType.OPENAI) "OpenAI key" else "Cloudflare token"}")
            }
        }

        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = onSave,
            enabled = !state.saving && !state.testing && !state.importingModel,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.saving) "Saving…" else "Save") }

        OutlinedButton(
            onClick = onTest,
            enabled = (state.isOfflineLocal || state.remoteEnabled) && !state.saving && !state.testing && !state.importingModel,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.testing) "Testing…" else if (state.isOfflineLocal) "Test local provider" else "Test connection") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun CredentialField(
    state: AiSettingsUiState,
    label: String,
    placeholder: String,
    onCredentialChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = state.credentialDraft,
        onValueChange = onCredentialChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        enabled = state.remoteEnabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
