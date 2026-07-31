# Codex instructions for Mitra

Read `ARCHITECTURE.md` before architectural changes.

## Fixed constraints
- Native Android, Kotlin, Jetpack Compose.
- Local-first single-child app.
- Room + DataStore + app-private files only.
- No Firebase, Supabase, PostgreSQL, account system, backend, ads, web browser, YouTube, feeds or streak mechanics.
- Imported PDFs are copied into `files/books/{bookId}/source.pdf`.
- Parent-only configuration is PIN protected.
- Do not embed cloud API secrets in source or BuildConfig.
- `LearningEngine` controls curriculum, answer evaluation and mastery.
- `AiGateway` controls presentation/content only; it must never assign mastery.
- `MockAiGateway` must keep learning sessions usable without internet.
- Voice input/output must remain replaceable behind `SpeechInput` and `SpeechOutput`.
- Text answer entry must continue working when voice is unavailable or permission is denied.

## Development process
Implement one milestone at a time. For every milestone:
1. Keep domain/business logic outside Compose functions.
2. Add/update tests.
3. Run `gradle testDebugUnitTest`.
4. Run `gradle lintDebug`.
5. Run `gradle assembleDebug`.
6. Do not disable tests or lint just to make builds pass.
7. Preserve Room data with explicit migrations; never use destructive migration in release behavior.

## Current scope
Milestone 3 is implemented: local curriculum/mastery/session logic plus push-to-talk Gujarati speech recognition and Gujarati TTS with text fallback.

Before starting another milestone, verify Milestone 3 is green in GitHub Actions and on a physical Android device.

## Next scope — Milestone 4 only
Implement book understanding behind `AiGateway`:
- table-of-contents page selection
- editable chapter structure
- chapter preparation
- page knowledge
- concept extraction

Do not add continuous/full-duplex voice or a parent analytics dashboard in Milestone 4.
