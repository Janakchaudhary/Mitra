# Milestone 3 release notes

Version: 0.3.0

## Added
- Gujarati push-to-talk voice input through Android `SpeechRecognizer`.
- Runtime microphone permission flow with text fallback when denied.
- Gujarati text-to-speech through Android `TextToSpeech` using `gu-IN`.
- Spoken question prompts, spoken feedback, and spoken session completion message.
- "ફરી સાંભળો" replay control for the current question.
- Partial speech transcript display while listening.
- Recognized answers are submitted to the existing deterministic learning engine.
- Spoken stop commands including `બસ`, `બંધ`, and `રોકો`.
- Replaceable `SpeechInput` and `SpeechOutput` interfaces.
- Android implementations plus fake voice implementations in unit tests.
- Unit tests for spoken-answer submission and spoken-stop behavior.

## Compatibility
- No Room schema change in Milestone 3.
- Existing Milestone 2 books, PIN, mastery, sessions, and attempts remain compatible.
- Voice is optional. The existing text input remains available.
- No cloud AI provider or API key is introduced.

## Device behavior
- Voice recognition availability depends on the speech recognition service installed on the Android device.
- Gujarati TTS availability depends on the installed TTS engine/language data.
- If either voice feature is unavailable, the child can continue with on-screen Gujarati text and typed answers.

## Intentionally not included
- Remote AI/API integration.
- PDF chapter understanding.
- Parent progress dashboard.
- Continuous/full-duplex voice conversation.
- Cloud speech provider fallback.
