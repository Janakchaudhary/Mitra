package com.mitra.learning.ui.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.learning.engine.LearningEngine
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.learning.model.SessionPlan
import com.mitra.learning.learning.model.SessionSummary
import com.mitra.learning.voice.SpeechInput
import com.mitra.learning.voice.SpeechInputState
import com.mitra.learning.voice.SpeechOutput
import com.mitra.learning.voice.SpeechOutputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearningSessionUiState(
    val loading: Boolean = true,
    val sessionId: String? = null,
    val conceptId: String? = null,
    val conceptTitleGujarati: String = "",
    val questions: List<LearningQuestion> = emptyList(),
    val questionIndex: Int = 0,
    val answer: String = "",
    val feedback: String? = null,
    val lastResult: AttemptResult? = null,
    val awaitingNext: Boolean = false,
    val summary: SessionSummary? = null,
    val error: String? = null,
    val speechInputAvailable: Boolean = false,
    val listening: Boolean = false,
    val partialTranscript: String = "",
    val ttsSpeaking: Boolean = false,
    val ttsAvailable: Boolean? = null,
    val voiceMessage: String? = null,
    val exitRequested: Boolean = false,
) {
    val currentQuestion: LearningQuestion?
        get() = questions.getOrNull(questionIndex)

    val completed: Boolean
        get() = summary != null
}

class LearningSessionViewModel(
    private val engine: LearningEngine,
    private val speechInput: SpeechInput,
    private val speechOutput: SpeechOutput,
) : ViewModel() {
    private val _state = MutableStateFlow(
        LearningSessionUiState(speechInputAvailable = speechInput.isAvailable)
    )
    val state: StateFlow<LearningSessionUiState> = _state.asStateFlow()

    init {
        observeSpeechInput()
        observeSpeechOutput()
        start()
    }

    fun updateAnswer(value: String) {
        if (_state.value.awaitingNext) return
        _state.update { it.copy(answer = value.take(32), error = null) }
    }

    fun submit() {
        val answer = _state.value.answer
        if (answer.isBlank()) {
            _state.update { it.copy(error = "જવાબ લખો, બોલો અથવા છોડો દબાવો.") }
            return
        }
        submitAnswer(answer)
    }

    fun skip() {
        val current = _state.value
        val sessionId = current.sessionId ?: return
        val conceptId = current.conceptId ?: return
        val question = current.currentQuestion ?: return
        if (current.awaitingNext || current.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { engine.skipQuestion(sessionId, conceptId, question) }
                .onSuccess { feedback ->
                    _state.update {
                        it.copy(
                            loading = false,
                            feedback = feedback.messageGujarati,
                            lastResult = feedback.result,
                            awaitingNext = true,
                        )
                    }
                    speak(feedback.messageGujarati)
                }
                .onFailure { failure ->
                    _state.update { it.copy(loading = false, error = failure.message ?: "કંઈક ખોટું થયું.") }
                }
        }
    }

    fun next() {
        val current = _state.value
        if (!current.awaitingNext || current.loading) return

        if (current.questionIndex >= current.questions.lastIndex) {
            complete()
        } else {
            val nextIndex = current.questionIndex + 1
            _state.update {
                it.copy(
                    questionIndex = nextIndex,
                    answer = "",
                    feedback = null,
                    lastResult = null,
                    awaitingNext = false,
                    error = null,
                    partialTranscript = "",
                    voiceMessage = null,
                )
            }
            current.questions.getOrNull(nextIndex)?.let { speak(it.promptGujarati) }
        }
    }

    fun startVoiceInput() {
        val current = _state.value
        if (current.loading || current.awaitingNext || current.completed) return
        if (!speechInput.isAvailable) {
            _state.update {
                it.copy(voiceMessage = "આ ફોનમાં અવાજ ઓળખવાની સુવિધા ઉપલબ્ધ નથી. લખીને જવાબ આપો.")
            }
            return
        }

        speechOutput.stop()
        viewModelScope.launch {
            runCatching { speechInput.startListening() }
                .onFailure {
                    _state.update { state ->
                        state.copy(voiceMessage = "માઇક્રોફોન શરૂ થઈ શક્યો નહીં. લખીને જવાબ આપો.")
                    }
                }
        }
    }

    fun stopVoiceInput() {
        viewModelScope.launch {
            runCatching { speechInput.stopListening() }
        }
    }

    fun onMicrophonePermissionDenied() {
        _state.update {
            it.copy(
                listening = false,
                voiceMessage = "માઇક્રોફોનની પરવાનગી નથી. લખીને જવાબ આપી શકો.",
            )
        }
    }

    fun replayPrompt() {
        _state.value.currentQuestion?.let { speak(it.promptGujarati) }
    }

    fun stop(onStopped: () -> Unit) {
        speechInput.cancel()
        speechOutput.stop()
        val id = _state.value.sessionId
        if (id == null) {
            onStopped()
            return
        }
        viewModelScope.launch {
            runCatching { engine.stopSession(id) }
            onStopped()
        }
    }

    private fun submitAnswer(answerText: String) {
        val current = _state.value
        val sessionId = current.sessionId ?: return
        val conceptId = current.conceptId ?: return
        val question = current.currentQuestion ?: return
        if (current.awaitingNext || current.loading) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    listening = false,
                    partialTranscript = "",
                )
            }
            runCatching {
                engine.submitAnswer(
                    sessionId = sessionId,
                    conceptId = conceptId,
                    question = question,
                    answerText = answerText,
                )
            }.onSuccess { feedback ->
                _state.update {
                    it.copy(
                        loading = false,
                        answer = answerText,
                        feedback = feedback.messageGujarati,
                        lastResult = feedback.result,
                        awaitingNext = true,
                    )
                }
                speak(feedback.messageGujarati)
            }.onFailure { failure ->
                _state.update { it.copy(loading = false, error = failure.message ?: "કંઈક ખોટું થયું.") }
            }
        }
    }

    private fun start() {
        viewModelScope.launch {
            runCatching { engine.startSession() }
                .onSuccess { plan -> applyPlan(plan) }
                .onFailure { failure ->
                    _state.update { it.copy(loading = false, error = failure.message ?: "સત્ર શરૂ થઈ શક્યું નહીં.") }
                }
        }
    }

    private fun applyPlan(plan: SessionPlan?) {
        if (plan == null) {
            _state.update { it.copy(loading = false, error = "હજુ શીખવા માટે કોઈ વિષય તૈયાર નથી.") }
            return
        }
        _state.value = LearningSessionUiState(
            loading = false,
            sessionId = plan.sessionId,
            conceptId = plan.concept.id,
            conceptTitleGujarati = plan.concept.titleGujarati,
            questions = plan.questions,
            speechInputAvailable = speechInput.isAvailable,
            ttsAvailable = _state.value.ttsAvailable,
        )
        plan.questions.firstOrNull()?.let { speak(it.promptGujarati) }
    }

    private fun complete() {
        val current = _state.value
        val sessionId = current.sessionId ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { engine.completeSession(sessionId, current.conceptTitleGujarati) }
                .onSuccess { summary ->
                    _state.update { it.copy(loading = false, summary = summary, awaitingNext = false) }
                    speak("આજની રમત પૂરી! હવે ફોનને થોડો આરામ આપીએ.")
                }
                .onFailure { failure ->
                    _state.update { it.copy(loading = false, error = failure.message ?: "સત્ર પૂરુ થઈ શક્યું નહીં.") }
                }
        }
    }

    private fun observeSpeechInput() {
        viewModelScope.launch {
            speechInput.state.collect { speechState ->
                when (speechState) {
                    SpeechInputState.Idle -> _state.update {
                        it.copy(listening = false, partialTranscript = "")
                    }
                    SpeechInputState.Listening -> _state.update {
                        it.copy(
                            listening = true,
                            partialTranscript = "",
                            voiceMessage = "સાંભળું છું…",
                        )
                    }
                    is SpeechInputState.Partial -> _state.update {
                        it.copy(
                            listening = true,
                            partialTranscript = speechState.text,
                            voiceMessage = "સાંભળું છું…",
                        )
                    }
                    is SpeechInputState.Result -> handleSpeechResult(speechState.text)
                    is SpeechInputState.Error -> _state.update {
                        it.copy(
                            listening = false,
                            partialTranscript = "",
                            voiceMessage = speechState.messageGujarati,
                        )
                    }
                }
            }
        }
    }

    private fun observeSpeechOutput() {
        viewModelScope.launch {
            speechOutput.state.collect { outputState ->
                when (outputState) {
                    SpeechOutputState.Initializing -> Unit
                    SpeechOutputState.Ready -> _state.update {
                        it.copy(ttsSpeaking = false, ttsAvailable = true)
                    }
                    SpeechOutputState.Speaking -> _state.update {
                        it.copy(ttsSpeaking = true, ttsAvailable = true)
                    }
                    SpeechOutputState.Unavailable -> _state.update {
                        it.copy(ttsSpeaking = false, ttsAvailable = false)
                    }
                    is SpeechOutputState.Error -> _state.update {
                        it.copy(
                            ttsSpeaking = false,
                            voiceMessage = outputState.messageGujarati,
                        )
                    }
                }
            }
        }
    }

    private fun handleSpeechResult(text: String) {
        val normalized = text.trim().lowercase()
        if (normalized in STOP_WORDS) {
            _state.update {
                it.copy(
                    listening = false,
                    partialTranscript = text,
                    voiceMessage = "બરાબર, આજ માટે બસ.",
                    exitRequested = true,
                )
            }
            return
        }

        _state.update {
            it.copy(
                answer = text.take(32),
                listening = false,
                partialTranscript = "",
                voiceMessage = "મેં સાંભળ્યું: $text",
            )
        }
        submitAnswer(text)
    }

    private fun speak(text: String) {
        viewModelScope.launch {
            runCatching { speechOutput.speakGujarati(text) }
        }
    }

    private companion object {
        val STOP_WORDS = setOf(
            "બસ",
            "બંધ",
            "બંધ કરો",
            "રોકો",
            "stop",
            "stop it",
        )
    }
}
