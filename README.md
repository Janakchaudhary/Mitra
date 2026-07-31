# Mitra / મિત્ર

A local-first Android learning companion for one Standard 2 Gujarati-medium child.

## What is implemented

### Milestone 1
- First launch parent PIN.
- Child and PIN-protected parent areas.
- Add PDF books through Android document picker.
- Copy PDF into app-private storage.
- SHA-256 duplicate detection.
- Room book library.
- Local `PdfRenderer` viewer.
- Remove a book and its private files.

### Milestone 2
- Room v1 → v2 migration that preserves existing books.
- Local curriculum/concept storage.
- Concept prerequisite model.
- Per-concept mastery tracking.
- Learning sessions and attempt history.
- Deterministic concept selection controlled by the app.
- Deterministic numeric answer evaluation, including Gujarati digits and common Gujarati number words.
- `AiGateway` abstraction plus `MockAiGateway`; no network/API key is required.
- Five built-in Standard 2 maths concepts for exercising the engine until book analysis is added.
- Child **રમીએ** flow with five-question Gujarati practice sessions.

### Milestone 3 — current
- Gujarati push-to-talk using Android `SpeechRecognizer`.
- Runtime microphone permission handling.
- Gujarati `TextToSpeech` using `gu-IN` when supported by the phone.
- Questions and feedback are spoken automatically.
- Current question can be replayed with **ફરી સાંભળો**.
- Child can hold the microphone control and speak the answer.
- Recognized speech is submitted to the same deterministic learning engine.
- Partial recognition text is visible while listening.
- Spoken **બસ / બંધ / રોકો** stops the session.
- Text answer entry remains available at all times as fallback.
- Voice features live behind replaceable `SpeechInput` / `SpeechOutput` interfaces.
- Unit tests cover spoken answer submission and spoken stop behavior.

No remote AI/API key is required yet.

## Build online with GitHub

1. Create/update a private GitHub repository with this project.
2. Push to `main`.
3. Open the **Actions** tab and run **Android build**.
4. Download the `mitra-debug-apk` artifact from the completed workflow.
5. Install `app-debug.apk` over the existing Mitra APK.

The workflow runs:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

## Develop in Codespaces

The included `.devcontainer` installs Java 17, Gradle and Android SDK 35. Codex can run the same Gradle checks from the browser workspace.

## Voice testing checklist

Use a physical Android phone when possible:

1. Start **રમીએ**.
2. Confirm the question is spoken in Gujarati if Gujarati TTS is installed.
3. Hold the microphone control and say a numeric answer such as **પાંચ**.
4. Confirm the recognized text appears and the answer is evaluated.
5. Say **બસ** and confirm the session exits.
6. Deny microphone permission and confirm typed answers still work.
7. Disable network temporarily and verify the learning session/text fallback still works.

## Next milestone

Milestone 4 should implement book understanding: contents-page selection, editable chapter structure, chapter preparation, page knowledge, and concept extraction behind the existing `AiGateway` boundary.
