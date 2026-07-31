package com.mitra.learning.ui.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun LearningSessionScreen(
    state: LearningSessionUiState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onDone: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            state.completed -> CompletedContent(state, onDone)
            state.loading && state.sessionId == null -> LoadingContent()
            else -> SessionContent(
                state = state,
                onAnswerChange = onAnswerChange,
                onSubmit = onSubmit,
                onSkip = onSkip,
                onNext = onNext,
                onStop = onStop,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("રમત તૈયાર થઈ રહી છે…")
    }
}

@Composable
private fun SessionContent(
    state: LearningSessionUiState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("🦁 મિત્ર", style = MaterialTheme.typography.headlineMedium)
        if (state.conceptTitleGujarati.isNotBlank()) {
            Text(state.conceptTitleGujarati, style = MaterialTheme.typography.titleMedium)
        }

        val current = state.currentQuestion
        if (current != null) {
            Text(
                "પ્રશ્ન ${state.questionIndex + 1} / ${state.questions.size}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(current.promptGujarati, style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = state.answer,
                onValueChange = onAnswerChange,
                label = { Text("તમારો જવાબ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !state.awaitingNext && !state.loading,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            state.feedback?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (state.awaitingNext) {
                Button(
                    onClick = onNext,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.questionIndex == state.questions.lastIndex) "પૂર્ણ કરીએ" else "આગળ")
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("જવાબ આપો")
                }
                OutlinedButton(
                    onClick = onSkip,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("આ પ્રશ્ન છોડો")
                }
            }
        } else {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = onStop, enabled = !state.loading) {
                Text("■ બસ")
            }
        }
    }
}

@Composable
private fun CompletedContent(
    state: LearningSessionUiState,
    onDone: () -> Unit,
) {
    val summary = requireNotNull(state.summary)
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🌟", style = MaterialTheme.typography.displayLarge)
        Text("આજની રમત પૂરી!", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(summary.conceptTitleGujarati, style = MaterialTheme.typography.titleLarge)
        Text("${summary.correct} / ${summary.attempts} જવાબ સાચા")
        Spacer(Modifier.height(24.dp))
        Text("હવે ફોનને થોડો આરામ આપીએ. 😊")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("ઘરે પાછા")
        }
    }
}
