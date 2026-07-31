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
- `MockAiGateway` must keep Milestone 2 usable without internet.

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
Milestone 2 is implemented: local curriculum/concepts/prerequisites, mastery, sessions, attempts, learning engine, deterministic numeric evaluation, `AiGateway` + `MockAiGateway`, and a text-based Gujarati practice session.

Verify Milestone 2 is green before beginning Milestone 3.

## Next scope — Milestone 3 only
Add push-to-talk Gujarati speech input/output behind interfaces. Keep text entry working as fallback. Do not add remote AI or book analysis in Milestone 3.
