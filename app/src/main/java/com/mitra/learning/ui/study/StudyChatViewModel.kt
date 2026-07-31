package com.mitra.learning.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.ai.AiGateway
import com.mitra.learning.study.StudyChatTurn
import com.mitra.learning.study.StudyContextService
import com.mitra.learning.study.StudyQuestionRequest
import com.mitra.learning.voice.SpeechInput
import com.mitra.learning.voice.SpeechInputState
import com.mitra.learning.voice.SpeechOutput
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StudySpeaker { CHILD, MITRA }

data class StudyMessage(
    val id: String = UUID.randomUUID().toString(),
    val speaker: StudySpeaker,
    val text: String,
    val sources: List<String> = emptyList(),
)

data class StudyChatUiState(
    val input: String = "",
    val messages: List<StudyMessage> = emptyList(),
    val loading: Boolean = false,
    val listening: Boolean = false,
    val partialTranscript: String = "",
    val speechAvailable: Boolean = true,
    val preparedBooksAvailable: Boolean = true,
    val error: String? = null,
)

class StudyChatViewModel(
    private val contextService: StudyContextService,
    private val aiGateway: AiGateway,
    private val speechInput: SpeechInput,
    private val speechOutput: SpeechOutput,
) : ViewModel() {
    private val _state = MutableStateFlow(StudyChatUiState(speechAvailable = speechInput.isAvailable))
    val state: StateFlow<StudyChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                preparedBooksAvailable = contextService.hasPreparedStudyMaterial(),
            )
        }
        viewModelScope.launch {
            speechInput.state.collect { state ->
                when (state) {
                    SpeechInputState.Idle -> _state.value = _state.value.copy(listening = false)
                    SpeechInputState.Listening -> _state.value = _state.value.copy(listening = true, error = null)
                    is SpeechInputState.Partial -> _state.value = _state.value.copy(
                        listening = true,
                        partialTranscript = state.text,
                    )
                    is SpeechInputState.Result -> {
                        _state.value = _state.value.copy(
                            listening = false,
                            partialTranscript = "",
                            input = state.text,
                        )
                        ask(state.text)
                    }
                    is SpeechInputState.Error -> _state.value = _state.value.copy(
                        listening = false,
                        partialTranscript = "",
                        error = state.messageGujarati,
                    )
                }
            }
        }
    }

    fun updateInput(value: String) {
        _state.value = _state.value.copy(input = value.take(300), error = null)
    }

    fun askCurrent() = ask(_state.value.input)

    fun ask(questionRaw: String) {
        val question = questionRaw.trim()
        if (question.isBlank() || _state.value.loading) return
        viewModelScope.launch {
            val childMessage = StudyMessage(speaker = StudySpeaker.CHILD, text = question)
            _state.value = _state.value.copy(
                input = "",
                loading = true,
                error = null,
                messages = (_state.value.messages + childMessage).takeLast(16),
            )
            runCatching {
                val sources = contextService.findSources(question)
                val history = _state.value.messages
                    .windowed(2, 2, partialWindows = false)
                    .mapNotNull { pair ->
                        val child = pair.getOrNull(0)?.takeIf { it.speaker == StudySpeaker.CHILD }
                        val mitra = pair.getOrNull(1)?.takeIf { it.speaker == StudySpeaker.MITRA }
                        if (child != null && mitra != null) StudyChatTurn(child.text, mitra.text) else null
                    }
                    .takeLast(4)
                aiGateway.answerStudyQuestion(
                    StudyQuestionRequest(
                        question = question,
                        sources = sources,
                        recentTurns = history,
                    )
                )
            }.onSuccess { answer ->
                val combined = buildString {
                    append(answer.answerGujarati.trim())
                    answer.followUpGujarati?.takeIf { it.isNotBlank() }?.let {
                        append("\n\n")
                        append(it.trim())
                    }
                }
                _state.value = _state.value.copy(
                    loading = false,
                    messages = (_state.value.messages + StudyMessage(
                        speaker = StudySpeaker.MITRA,
                        text = combined,
                        sources = answer.sourceLabels,
                    )).takeLast(16),
                )
                speechOutput.speakGujarati(combined)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = error.message ?: "મિત્ર હમણાં પુસ્તકમાંથી જવાબ શોધી શક્યો નહીં.",
                )
            }
        }
    }

    fun startVoice() = viewModelScope.launch {
        speechOutput.stop()
        speechInput.startListening("gu-IN")
    }

    fun stopVoice() = viewModelScope.launch { speechInput.stopListening() }

    fun microphoneDenied() {
        _state.value = _state.value.copy(error = "માઇક્રોફોનની પરવાનગી આપો અથવા સવાલ લખો.")
    }

    fun replayLastAnswer() {
        val last = _state.value.messages.lastOrNull { it.speaker == StudySpeaker.MITRA } ?: return
        viewModelScope.launch { speechOutput.speakGujarati(last.text) }
    }

    override fun onCleared() {
        speechInput.cancel()
        speechOutput.stop()
        super.onCleared()
    }
}
