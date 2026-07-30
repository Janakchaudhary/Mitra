package com.mitra.learning.ui.books

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    state: AddBookUiState,
    onPdfSelected: (Uri) -> Unit,
    onTitleChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onPdfSelected)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add book") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                Text(state.selectedFileName ?: "Choose PDF")
            }
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text("Book name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.subject,
                onValueChange = onSubjectChange,
                label = { Text("Subject") },
                supportingText = { Text("Example: Gujarati, Mathematics, આસપાસ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("Standard: 2")
            Text("Medium: Gujarati")
            state.error?.let { Text(it) }
            Button(
                onClick = onImport,
                enabled = !state.importing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.importing) CircularProgressIndicator() else Text("Add to Mitra")
            }
        }
    }
}
