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
Milestone 7 is implemented. The app includes real book analysis, rich child-safe learning activities, local mastery/session tracking, and a parent-only progress dashboard.

Before starting another milestone, verify Milestone 7 is green in GitHub Actions and on a physical Android device.

## Next scope — Milestone 8 only
Polish the personal-use APK without changing the local-first architecture. Priorities: session/daily limits, parent settings, parent relock behavior, offline/error UX, accessibility, Gujarati UI copy, data-reset controls, and final physical-device hardening.

Do not introduce a backend, account system, ads, browser, feeds, or cloud progress storage.
