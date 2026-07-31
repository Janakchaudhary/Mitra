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

### Milestone 2 — current
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
- Skip, stop, feedback and completion behavior.
- Unit tests for mastery, prerequisite selection, answer normalization and learning engine persistence.

Voice and real AI are intentionally not included yet.

## Build online with GitHub

1. Create/update a private GitHub repository with this project.
2. Push to `main`.
3. Open the **Actions** tab and run **Android build**.
4. Download the `mitra-debug-apk` artifact from the completed workflow.
5. Install `app-debug.apk` on your Android phone.

The workflow runs:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

## Develop in Codespaces

The included `.devcontainer` installs Java 17, Gradle and Android SDK 35. Codex can run the same Gradle checks from the browser workspace.

## Next task for Codex

Verify Milestone 2 is green in GitHub Actions. Then begin Milestone 3 only: add push-to-talk Gujarati speech input/output behind replaceable interfaces, while keeping the current text practice flow as a fallback.
