package com.mitra.learning.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.ai.AiGateway
import com.mitra.learning.learning.limits.LearningLimitService
import com.mitra.learning.study.StudyChatTurn
import com.mitra.learning.study.StudyContextService
import com.mitra.learning.study.StudyLocalResponder
import com.mitra.learning.study.StudyQuestionRequest
import com.mitra.learning.study.StudyResponseKind
import com.mitra.learning.study.practice.MitraPracticeTopic
import com.mitra.learning.study.practice.MitraVoiceChallenge
import com.mitra.learning.study.practice.MitraVoicePracticeService
import com.mitra.learning.voice.SpeechInput
import com.mitra.learning.voice.SpeechInputState
import com.mitra.learning.voice.SpeechOutput
import com.mitra.learning.voice.SpeechOutputState
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val responseKind: StudyResponseKind? = null,
)

data class StudyChatUiState(
    val input: String = "",
    val messages: List<StudyMessage> = emptyList(),
    val loading: Boolean = false,
    val listening: Boolean = false,
    val partialTranscript: String = "",
    val speechAvailable: Boolean = true,
    val preparedBooksAvailable: Boolean = true,
    val handsFreeEnabled: Boolean = false,
    val waitingForNextTurn: Boolean = false,
    val activeChallenge: MitraVoiceChallenge? = null,
    val practiceTopic: MitraPracticeTopic? = null,
    val challengeAttempts: Int = 0,
    val correctCount: Int = 0,
    val correctStreak: Int = 0,
    val remainingSeconds: Int? = null,
    val timeLimitReached: Boolean = false,
    val error: String? = null,
)

class StudyChatViewModel(
    private val contextService: StudyContextService,
    private val aiGateway: AiGateway,
    private val speechInput: SpeechInput,
    private val speechOutput: SpeechOutput,
    private val practiceService: MitraVoicePracticeService,
    private val localResponder: StudyLocalResponder = StudyLocalResponder(),
    private val limitService: LearningLimitService? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(StudyChatUiState(speechAvailable = speechInput.isAvailable))
    val state: StateFlow<StudyChatUiState> = _state.asStateFlow()

    private var autoListenAfterSpeech = false
    private var speechStartedForAutoTurn = false
    private var countdownJob: Job? = null
    private var recoveryJob: Job? = null

    init {
        viewModelScope.launch {
            val limits = limitService?.status()
            _state.value = _state.value.copy(
                preparedBooksAvailable = contextService.hasPreparedStudyMaterial(),
                remainingSeconds = limits?.sessionLimitSeconds,
                timeLimitReached = limits?.canStart == false,
                error = if (limits?.canStart == false) "આજનો શીખવાનો સમય પૂરો થયો. હવે ફોનને આરામ આપીએ." else null,
            )
            if (limits?.canStart != false) {
                limits?.sessionLimitSeconds?.takeIf { it > 0 }?.let(::startCountdown)
            }
        }
        viewModelScope.launch {
            speechInput.state.collect { inputState ->
                when (inputState) {
                    SpeechInputState.Idle -> _state.value = _state.value.copy(listening = false)
                    SpeechInputState.Listening -> _state.value = _state.value.copy(
                        listening = true,
                        waitingForNextTurn = false,
                        error = null,
                    )
                    is SpeechInputState.Partial -> _state.value = _state.value.copy(
                        listening = true,
                        partialTranscript = inputState.text,
                    )
                    is SpeechInputState.Result -> {
                        _state.value = _state.value.copy(
                            listening = false,
                            partialTranscript = "",
                            input = inputState.text,
                        )
                        ask(inputState.text)
                    }
                    is SpeechInputState.Error -> {
                        val shouldRecover = _state.value.handsFreeEnabled &&
                            inputState.recoverable && !_state.value.loading && !_state.value.timeLimitReached
                        _state.value = _state.value.copy(
                            listening = false,
                            waitingForNextTurn = shouldRecover,
                            partialTranscript = "",
                            error = inputState.messageGujarati,
                        )
                        recoveryJob?.cancel()
                        if (shouldRecover) {
                            recoveryJob = viewModelScope.launch {
                                delay(900)
                                if (_state.value.handsFreeEnabled && !_state.value.loading && !_state.value.timeLimitReached) {
                                    startListeningInternal()
                                }
                            }
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            speechOutput.state.collect { outputState ->
                when (outputState) {
                    SpeechOutputState.Speaking -> {
                        if (autoListenAfterSpeech) speechStartedForAutoTurn = true
                    }
                    SpeechOutputState.Ready -> {
                        if (autoListenAfterSpeech && speechStartedForAutoTurn && _state.value.handsFreeEnabled) {
                            autoListenAfterSpeech = false
                            speechStartedForAutoTurn = false
                            delay(350)
                            startListeningInternal()
                        }
                    }
                    is SpeechOutputState.Error,
                    SpeechOutputState.Unavailable,
                    -> {
                        autoListenAfterSpeech = false
                        speechStartedForAutoTurn = false
                        _state.value = _state.value.copy(waitingForNextTurn = false)
                    }
                    SpeechOutputState.Initializing -> Unit
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
        if (question.isBlank() || _state.value.loading || _state.value.timeLimitReached) return

        if (localResponder.isStopRequest(question)) {
            stopPractice(speakClosing = false)
            setHandsFree(false)
            return
        }

        _state.value.activeChallenge?.let { challenge ->
            answerChallenge(challenge, question)
            return
        }

        practiceService.detectTopic(question)?.let { topic ->
            beginPractice(topic, childRequest = question)
            return
        }

        askStudyQuestion(question)
    }

    fun startPractice(topic: MitraPracticeTopic) {
        if (_state.value.loading || _state.value.timeLimitReached) return
        beginPractice(topic, childRequest = null)
    }

    fun stopPractice(speakClosing: Boolean = true) {
        val wasActive = _state.value.activeChallenge != null || _state.value.practiceTopic != null
        _state.value = _state.value.copy(
            activeChallenge = null,
            practiceTopic = null,
            challengeAttempts = 0,
            correctStreak = 0,
            waitingForNextTurn = false,
        )
        if (wasActive && speakClosing) {
            val closing = "બરાબર, voice પ્રશ્ન રમત અહીં રોકીએ. હવે તમે પુસ્તક વિશે કોઈપણ સવાલ પૂછો."
            appendMitraMessage(closing, StudyResponseKind.VOICE_PRACTICE)
            viewModelScope.launch { speechOutput.speakGujarati(closing) }
        }
    }

    private fun beginPractice(topic: MitraPracticeTopic, childRequest: String?) {
        viewModelScope.launch {
            childRequest?.let { appendChildMessage(it) }
            _state.value = _state.value.copy(
                input = "",
                loading = true,
                listening = false,
                waitingForNextTurn = false,
                error = null,
                practiceTopic = topic,
                challengeAttempts = 0,
                correctStreak = 0,
            )
            runCatching { practiceService.nextChallenge(topic) }
                .onSuccess { challenge ->
                    if (challenge.id == "book-unavailable") {
                        _state.value = _state.value.copy(
                            loading = false,
                            activeChallenge = null,
                            practiceTopic = null,
                        )
                        appendMitraMessage(
                            text = challenge.promptGujarati,
                            kind = StudyResponseKind.VOICE_PRACTICE,
                        )
                        speechOutput.speakGujarati(challenge.spokenPrompt)
                    } else {
                        _state.value = _state.value.copy(
                            loading = false,
                            activeChallenge = challenge,
                            practiceTopic = topic,
                            challengeAttempts = 0,
                            waitingForNextTurn = _state.value.handsFreeEnabled,
                        )
                        appendMitraMessage(
                            text = "ચાલો ${topic.emoji} ${topic.titleGujarati} શરૂ કરીએ.\n\n${challenge.promptGujarati}",
                            kind = StudyResponseKind.VOICE_PRACTICE,
                            sources = challenge.sourceLabels,
                        )
                        speakForNextTurn(challenge.spokenPrompt, challenge.speechLanguageTag)
                    }
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        activeChallenge = null,
                        practiceTopic = null,
                        error = error.message ?: "voice પ્રશ્ન તૈયાર થઈ શક્યો નહીં.",
                    )
                }
        }
    }

    private fun answerChallenge(challenge: MitraVoiceChallenge, rawAnswer: String) {
        viewModelScope.launch {
            appendChildMessage(rawAnswer)
            _state.value = _state.value.copy(
                input = "",
                loading = true,
                listening = false,
                waitingForNextTurn = false,
                error = null,
            )
            val evaluation = practiceService.evaluate(challenge, rawAnswer, _state.value.correctStreak)
            val attempt = _state.value.challengeAttempts + 1

            if (!evaluation.correct && attempt < MAX_CHALLENGE_ATTEMPTS) {
                val retry = "${evaluation.feedbackGujarati}\n\nફરી પ્રયત્ન કરો: ${challenge.promptGujarati}"
                val spokenRetry = "${evaluation.feedbackGujarati}. ફરી પ્રયત્ન કરો. ${challenge.spokenPrompt}"
                _state.value = _state.value.copy(
                    loading = false,
                    activeChallenge = challenge,
                    challengeAttempts = attempt,
                    correctStreak = 0,
                    waitingForNextTurn = _state.value.handsFreeEnabled,
                )
                appendMitraMessage(retry, StudyResponseKind.VOICE_PRACTICE, challenge.sourceLabels)
                speakForNextTurn(spokenRetry)
                return@launch
            }

            val feedback = if (evaluation.correct) {
                evaluation.feedbackGujarati
            } else {
                "${evaluation.feedbackGujarati} ${practiceService.revealedCorrection(challenge)}"
            }
            val selectedTopic = _state.value.practiceTopic ?: challenge.topic
            val next = runCatching {
                practiceService.nextChallenge(selectedTopic, previousChallengeId = challenge.id)
            }.getOrNull()

            if (next == null || next.id == "book-unavailable") {
                val closing = "$feedback\n\nઆ રાઉન્ડ પૂરો થયો. હવે બીજો વિષય પસંદ કરો અથવા પુસ્તકનો સવાલ પૂછો."
                _state.value = _state.value.copy(
                    loading = false,
                    activeChallenge = null,
                    practiceTopic = null,
                    challengeAttempts = 0,
                    correctCount = _state.value.correctCount + if (evaluation.correct) 1 else 0,
                    correctStreak = if (evaluation.correct) _state.value.correctStreak + 1 else 0,
                )
                appendMitraMessage(closing, StudyResponseKind.VOICE_PRACTICE, challenge.sourceLabels)
                speechOutput.speakGujarati(closing)
                return@launch
            }

            val nextText = "$feedback\n\nઆગળનો પ્રશ્ન: ${next.promptGujarati}"
            val nextSpokenText = "$feedback. આગળનો પ્રશ્ન. ${next.spokenPrompt}"
            _state.value = _state.value.copy(
                loading = false,
                activeChallenge = next,
                challengeAttempts = 0,
                correctCount = _state.value.correctCount + if (evaluation.correct) 1 else 0,
                correctStreak = if (evaluation.correct) _state.value.correctStreak + 1 else 0,
                waitingForNextTurn = _state.value.handsFreeEnabled,
            )
            appendMitraMessage(
                nextText,
                StudyResponseKind.VOICE_PRACTICE,
                (challenge.sourceLabels + next.sourceLabels).distinct(),
            )
            speakForNextTurn(nextSpokenText)
        }
    }

    private fun askStudyQuestion(question: String) {
        val priorMessages = _state.value.messages
        viewModelScope.launch {
            appendChildMessage(question)
            _state.value = _state.value.copy(
                input = "",
                loading = true,
                listening = false,
                waitingForNextTurn = false,
                error = null,
            )
            runCatching {
                localResponder.respond(question) ?: run {
                    val previousChildQuestion = priorMessages.lastOrNull { it.speaker == StudySpeaker.CHILD }?.text
                    val retrievalQuestion = contextualStudyQuery(question, previousChildQuestion)
                    val sources = contextService.findSources(retrievalQuestion)
                    val history = priorMessages
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
                }
            }.onSuccess { answer ->
                val combined = buildString {
                    append(answer.answerGujarati.trim())
                    answer.followUpGujarati?.takeIf { it.isNotBlank() }?.let {
                        append("\n\n")
                        append(it.trim())
                    }
                }
                val continueHandsFree = _state.value.handsFreeEnabled && !answer.endConversation
                _state.value = _state.value.copy(
                    loading = false,
                    waitingForNextTurn = continueHandsFree,
                )
                appendMitraMessage(
                    text = combined,
                    kind = answer.responseKind,
                    sources = answer.sourceLabels,
                )
                autoListenAfterSpeech = continueHandsFree
                speechStartedForAutoTurn = false
                speechOutput.speakGujarati(combined)
            }.onFailure { error ->
                autoListenAfterSpeech = false
                speechStartedForAutoTurn = false
                _state.value = _state.value.copy(
                    loading = false,
                    waitingForNextTurn = false,
                    error = error.message ?: "મિત્ર હમણાં જવાબ શોધી શક્યો નહીં.",
                )
            }
        }
    }

    private fun appendChildMessage(text: String) {
        _state.value = _state.value.copy(
            messages = (_state.value.messages + StudyMessage(
                speaker = StudySpeaker.CHILD,
                text = text,
            )).takeLast(MAX_MESSAGES),
        )
    }

    private fun appendMitraMessage(
        text: String,
        kind: StudyResponseKind,
        sources: List<String> = emptyList(),
    ) {
        _state.value = _state.value.copy(
            messages = (_state.value.messages + StudyMessage(
                speaker = StudySpeaker.MITRA,
                text = text,
                sources = sources,
                responseKind = kind,
            )).takeLast(MAX_MESSAGES),
        )
    }

    private fun speakForNextTurn(text: String, speechLanguageTag: String = "gu-IN") {
        val continueHandsFree = _state.value.handsFreeEnabled && !_state.value.timeLimitReached
        autoListenAfterSpeech = continueHandsFree
        speechStartedForAutoTurn = false
        viewModelScope.launch { speechOutput.speak(text, speechLanguageTag) }
    }

    private fun contextualStudyQuery(question: String, previousChildQuestion: String?): String {
        val normalized = question.lowercase().trim()
        val vagueFollowUp = normalized.length < 28 && listOf(
            "એના", "તેના", "આના", "એ વિષે", "તે વિષે", "આ વિષે", "વધારે કહો", "ફરી સમજાવો",
            "about it", "tell me more", "explain again",
        ).any { normalized.contains(it) }
        return if (vagueFollowUp && !previousChildQuestion.isNullOrBlank()) {
            "$previousChildQuestion $question"
        } else question
    }

    fun setHandsFree(enabled: Boolean) {
        autoListenAfterSpeech = false
        recoveryJob?.cancel()
        speechStartedForAutoTurn = false
        if (!enabled) {
            speechInput.cancel()
            _state.value = _state.value.copy(
                handsFreeEnabled = false,
                listening = false,
                waitingForNextTurn = false,
                partialTranscript = "",
            )
            return
        }
        if (_state.value.timeLimitReached) {
            _state.value = _state.value.copy(error = "આ વાતનો સમય પૂરો થયો. હવે ફોનને આરામ આપીએ.")
            return
        }
        if (!speechInput.isAvailable) {
            _state.value = _state.value.copy(error = "આ ફોનમાં સતત voice વાત ઉપલબ્ધ નથી.")
            return
        }
        _state.value = _state.value.copy(handsFreeEnabled = true, error = null)
        viewModelScope.launch { startListeningInternal() }
    }

    fun startVoice() = viewModelScope.launch {
        speechOutput.stop()
        autoListenAfterSpeech = false
        speechStartedForAutoTurn = false
        startListeningInternal()
    }

    private suspend fun startListeningInternal() {
        if (_state.value.loading || _state.value.listening || _state.value.timeLimitReached || !speechInput.isAvailable) return
        _state.value = _state.value.copy(waitingForNextTurn = true, error = null)
        val languageTag = _state.value.activeChallenge?.recognitionLanguageTag ?: "gu-IN"
        runCatching { speechInput.startListening(languageTag) }
            .onFailure {
                _state.value = _state.value.copy(
                    listening = false,
                    waitingForNextTurn = false,
                    error = "માઇક્રોફોન શરૂ થઈ શક્યો નહીં. ફરી દબાવો અથવા લખીને પૂછો.",
                )
            }
    }

    fun stopVoice() = viewModelScope.launch {
        autoListenAfterSpeech = false
        speechStartedForAutoTurn = false
        speechInput.stopListening()
    }

    fun microphoneDenied() {
        setHandsFree(false)
        _state.value = _state.value.copy(error = "માઇક્રોફોનની પરવાનગી આપો અથવા સવાલ લખો.")
    }

    fun replayLastAnswer() {
        val last = _state.value.messages.lastOrNull { it.speaker == StudySpeaker.MITRA } ?: return
        viewModelScope.launch { speechOutput.speakGujarati(last.text) }
    }

    private fun startCountdown(totalSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && !_state.value.timeLimitReached) {
                _state.value = _state.value.copy(remainingSeconds = remaining)
                delay(1_000)
                remaining -= 1
            }
            if (remaining <= 0 && !_state.value.timeLimitReached) {
                setHandsFree(false)
                val closing = "આ વાતનો સમય પૂરો થયો. હવે ફોનને આરામ આપીએ અને પુસ્તક, ચિત્ર કે બહારની રમત તરફ જઈએ."
                _state.value = _state.value.copy(
                    remainingSeconds = 0,
                    timeLimitReached = true,
                    loading = false,
                    waitingForNextTurn = false,
                    activeChallenge = null,
                    practiceTopic = null,
                )
                appendMitraMessage(closing, StudyResponseKind.LOCAL_GUIDANCE)
                speechOutput.speakGujarati(closing)
            }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        recoveryJob?.cancel()
        speechInput.cancel()
        speechOutput.stop()
        super.onCleared()
    }

    private companion object {
        const val MAX_CHALLENGE_ATTEMPTS = 2
        const val MAX_MESSAGES = 30
    }
}
