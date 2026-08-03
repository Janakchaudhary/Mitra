# Milestone 19 — “મિત્રને પૂછીએ” voice tutor

Version: **0.19.0** (`versionCode 35`)

Milestone 19 turns the existing prepared-book Study Talk screen into a two-way spoken tutor for a Standard 2 child.

## Child capabilities

The child can still ask free-form questions. Mitra answers prepared-book questions using `StudyContextService` and the configured `AiGateway`, while local deterministic handling covers:

- ઘડિયા / પહાડા from 1 through 20
- previous and next number
- addition, subtraction, carrying, borrowing, comparison, and multiplication
- common Standard 2 English spelling

The same screen now provides explicit practice buttons:

- prepared-book question
- tables
- previous/next number
- spelling
- mixed practice

## Voice practice state machine

`StudyChatViewModel` holds one `MitraVoiceChallenge` at a time. The flow is:

1. `MitraVoicePracticeService` creates a short answerable challenge.
2. Android TTS speaks the prompt.
3. Hands-free mode automatically starts recognition after TTS; manual microphone input remains available.
4. Gujarati questions use `gu-IN`; English spelling replies use `en-IN`.
5. `MitraPracticeEvaluator` normalizes the transcript and evaluates it locally.
6. A correct answer receives varied praise and increments the streak.
7. A first wrong answer receives a targeted hint and retries the same question.
8. A second wrong answer receives the correct method/answer before a new question starts.

Raw audio is not stored.

## Prepared-book voice questions

`MitraVoicePracticeService` first loads voice-evaluable questions from the offline question bank created during chapter preparation. If none are cached, it calls `AiGateway.createPracticeQuestions` with the saved page text as `PracticeContext`.

`OfflineAiGateway` now asks an imported LiteRT-LM model for strict grounded question JSON. If no model is installed or model output is unusable, it creates conservative questions from the prepared concept title and learning outcome. It never replaces missing textbook evidence with web content.

## Speech recognition

`AndroidSpeechInput` prefers `SpeechRecognizer.createOnDeviceSpeechRecognizer` on Android 12+ when available and sets `RecognizerIntent.EXTRA_PREFER_OFFLINE`. Device language packs still control whether Gujarati and English recognition can run without a network connection.

## Data and migration

- Room schema remains version 5.
- No database migration is required.
- Existing books, prepared chapters, question banks, progress, settings, and parent credentials are preserved.
