package com.mitra.learning.ui.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.data.db.entity.AttemptResult
import com.mitra.learning.learning.engine.LearningEngine
import com.mitra.learning.learning.limits.LearningLimitService
import com.mitra.learning.learning.model.EvaluationMode
import com.mitra.learning.learning.model.LearningQuestion
import com.mitra.learning.learning.model.SessionPlan
import com.mitra.learning.learning.model.SessionSummary
import com.mitra.learning.voice.SpeechInput
import com.mitra.learning.voice.SpeechInputState
import com.mitra.learning.voice.SpeechOutput
import com.mitra.learning.voice.SpeechOutputState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val hintText: String? = null,
    val hintsUsed: Int = 0,
    val remainingSessionSeconds: Int? = null,
    val timeLimitReached: Boolean = false,
    val retryCount: Int = 0,
    val mistakeCode: String? = null,
    val pendingVoiceConfirmation: Boolean = false,
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
    private val limitService: LearningLimitService? = null,
    private val skillOnly: Boolean = false,
    private val requestedConceptId: String? = null,
) : ViewModel() {
    private var countdownJob: Job? = null
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
        _state.update { it.copy(answer = value.take(160), error = null, pendingVoiceConfirmation = false) }
    }

    fun submit() {
        val current = _state.value
        val activity = current.currentQuestion ?: return
        if (activity.evaluationMode == EvaluationMode.PARTICIPATION) {
            completeParticipation()
            return
        }
        if (current.answer.isBlank()) {
            _state.update { it.copy(error = "જવાબ લખો, બોલો અથવા છોડો દબાવો.") }
            return
        }
        submitAnswer(current.answer)
    }

    fun selectOption(option: String) {
        val current = _state.value
        if (current.loading || current.awaitingNext) return
        _state.update { it.copy(answer = option, error = null) }
        submitAnswer(option)
    }

    fun showHint() {
        val current = _state.value
        val hint = current.currentQuestion?.hintGujarati?.takeIf { it.isNotBlank() } ?: return
        if (current.awaitingNext || current.loading) return
        _state.update {
            it.copy(
                hintText = hint,
                hintsUsed = (it.hintsUsed + 1).coerceAtMost(3),
                error = null,
            )
        }
        speak("સંકેત: $hint")
    }

    fun completeParticipation() {
        val current = _state.value
        val sessionId = current.sessionId ?: return
        val activity = current.currentQuestion ?: return
        val conceptId = activity.conceptId ?: current.conceptId ?: return
        if (current.awaitingNext || current.loading) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, listening = false) }
            runCatching { engine.completeParticipation(sessionId, conceptId, activity) }
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

    fun skip() {
        val current = _state.value
        val sessionId = current.sessionId ?: return
        val question = current.currentQuestion ?: return
        val conceptId = question.conceptId ?: current.conceptId ?: return
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
                    hintText = null,
                    hintsUsed = 0,
                    retryCount = 0,
                    mistakeCode = null,
                    pendingVoiceConfirmation = false,
                )
            }
            current.questions.getOrNull(nextIndex)?.let(::speakActivity)
        }
    }

    fun startVoiceInput() {
        val current = _state.value
        if (current.loading || current.awaitingNext || current.completed) return
        if (current.currentQuestion?.type == com.mitra.learning.learning.model.ActivityType.SPELLING) {
            _state.update { it.copy(voiceMessage = "જોડણી માટે સાંભળેલા શબ્દને લખીને જવાબ આપો.") }
            return
        }
        if (!speechInput.isAvailable) {
            _state.update {
                it.copy(voiceMessage = "આ ફોનમાં અવાજ ઓળખવાની સુવિધા ઉપલબ્ધ નથી. લખીને જવાબ આપો.")
            }
            return
        }

        speechOutput.stop()
        viewModelScope.launch {
            val languageTag = current.currentQuestion?.recognitionLanguageTag?.takeIf { it.isNotBlank() } ?: "gu-IN"
            runCatching { speechInput.startListening(languageTag) }
                .onFailure {
                    _state.update { state ->
                        state.copy(voiceMessage = "માઇક્રોફોન શરૂ થઈ શક્યો નહીં. લખીને જવાબ આપો.")
                    }
                }
        }
    }

    fun stopVoiceInput() {
        viewModelScope.launch { runCatching { speechInput.stopListening() } }
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
        _state.value.currentQuestion?.let(::speakActivity)
    }

    fun stop(onStopped: () -> Unit) {
        countdownJob?.cancel()
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
        val question = current.currentQuestion ?: return
        val conceptId = question.conceptId ?: current.conceptId ?: return
        if (current.awaitingNext || current.loading) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    listening = false,
                    partialTranscript = "",
                    pendingVoiceConfirmation = false,
                )
            }
            runCatching {
                engine.submitAnswer(
                    sessionId = sessionId,
                    conceptId = conceptId,
                    question = question,
                    answerText = answerText,
                    hintsUsed = current.hintsUsed,
                )
            }.onSuccess { feedback ->
                val shouldRetry = feedback.retrySuggested && current.retryCount < 1
                val finalMessage = if (!shouldRetry && feedback.result == AttemptResult.INCORRECT && feedback.expectedAnswer != null) {
                    "${feedback.messageGujarati} સાચો જવાબ ${feedback.expectedAnswer} છે; હવે આગળના પ્રશ્નમાં આ પગલું યાદ રાખીએ."
                } else feedback.messageGujarati
                _state.update {
                    it.copy(
                        loading = false,
                        answer = if (shouldRetry) "" else answerText,
                        feedback = finalMessage,
                        hintText = if (shouldRetry) feedback.messageGujarati else it.hintText,
                        lastResult = feedback.result,
                        awaitingNext = !shouldRetry,
                        retryCount = if (shouldRetry) it.retryCount + 1 else it.retryCount,
                        hintsUsed = if (shouldRetry) (it.hintsUsed + 1).coerceAtMost(3) else it.hintsUsed,
                        mistakeCode = feedback.mistakeCode,
                    )
                }
                speak(finalMessage)
            }.onFailure { failure ->
                _state.update { it.copy(loading = false, error = failure.message ?: "કંઈક ખોટું થયું.") }
            }
        }
    }

    private fun start() {
        viewModelScope.launch {
            val limits = limitService?.status()
            if (limits != null && !limits.canStart) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "આજનો શીખવાનો સમય પૂરો થયો. હવે ફોનને આરામ આપીએ. 😊",
                        timeLimitReached = true,
                    )
                }
                return@launch
            }

            runCatching {
                when {
                    requestedConceptId != null -> engine.startConceptSession(requestedConceptId)
                    skillOnly -> engine.startSkillSession()
                    else -> engine.startSession()
                }
            }
                .onSuccess { plan -> applyPlan(plan, limits?.sessionLimitSeconds) }
                .onFailure { failure ->
                    _state.update { it.copy(loading = false, error = failure.message ?: "સત્ર શરૂ થઈ શક્યું નહીં.") }
                }
        }
    }

    private fun applyPlan(plan: SessionPlan?, sessionLimitSeconds: Int?) {
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
            remainingSessionSeconds = sessionLimitSeconds,
        )
        sessionLimitSeconds?.takeIf { it > 0 }?.let(::startCountdown)
        plan.questions.firstOrNull()?.let(::speakActivity)
    }

    private fun startCountdown(totalSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && !_state.value.completed) {
                _state.update { it.copy(remainingSessionSeconds = remaining) }
                delay(1_000L)
                remaining -= 1
            }
            if (remaining <= 0 && !_state.value.completed && _state.value.sessionId != null) {
                _state.update { it.copy(remainingSessionSeconds = 0, timeLimitReached = true) }
                complete(timeLimitReached = true)
            }
        }
    }

    private fun complete(timeLimitReached: Boolean = false) {
        val current = _state.value
        val sessionId = current.sessionId ?: return
        countdownJob?.cancel()
        speechInput.cancel()
        viewModelScope.launch {
            _state.update { it.copy(loading = true, timeLimitReached = it.timeLimitReached || timeLimitReached) }
            runCatching { engine.completeSession(sessionId, current.conceptTitleGujarati) }
                .onSuccess { summary ->
                    _state.update { it.copy(loading = false, summary = summary, awaitingNext = false) }
                    speak(
                        if (_state.value.timeLimitReached) "આજની રમતનો સમય પૂરો થયો! હવે ફોનને થોડો આરામ આપીએ."
                        else "આજની રમત પૂરી! હવે ફોનને થોડો આરામ આપીએ."
                    )
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
                    SpeechInputState.Idle -> _state.update { it.copy(listening = false, partialTranscript = "") }
                    SpeechInputState.Listening -> _state.update {
                        it.copy(listening = true, partialTranscript = "", voiceMessage = "સાંભળું છું…")
                    }
                    is SpeechInputState.Partial -> _state.update {
                        it.copy(listening = true, partialTranscript = speechState.text, voiceMessage = "સાંભળું છું…")
                    }
                    is SpeechInputState.Result -> handleSpeechResult(speechState.text)
                    is SpeechInputState.Error -> _state.update {
                        it.copy(listening = false, partialTranscript = "", voiceMessage = speechState.messageGujarati)
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
                    SpeechOutputState.Ready -> _state.update { it.copy(ttsSpeaking = false, ttsAvailable = true) }
                    SpeechOutputState.Speaking -> _state.update { it.copy(ttsSpeaking = true, ttsAvailable = true) }
                    SpeechOutputState.Unavailable -> _state.update { it.copy(ttsSpeaking = false, ttsAvailable = false) }
                    is SpeechOutputState.Error -> _state.update {
                        it.copy(ttsSpeaking = false, voiceMessage = outputState.messageGujarati)
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

        val activity = _state.value.currentQuestion
        _state.update {
            it.copy(
                answer = text.take(160),
                listening = false,
                partialTranscript = "",
                pendingVoiceConfirmation = activity?.evaluationMode != EvaluationMode.PARTICIPATION,
                voiceMessage = if (activity?.evaluationMode != EvaluationMode.PARTICIPATION)
                    "મેં સાંભળ્યું: $text • સાચું હોય તો ‘જવાબ ચકાસો’ દબાવો."
                else "મેં સાંભળ્યું: $text",
            )
        }
    }

    private fun speakActivity(question: LearningQuestion) {
        val languageTag = question.speechLanguageTag?.takeIf { it.isNotBlank() } ?: "gu-IN"
        viewModelScope.launch { runCatching { speechOutput.speak(question.speechTextGujarati, languageTag) } }
    }

    private fun speak(text: String) {
        viewModelScope.launch { runCatching { speechOutput.speakGujarati(text) } }
    }

    private companion object {
        val STOP_WORDS = setOf("બસ", "બંધ", "બંધ કરો", "રોકો", "stop", "stop it")
    }
}
