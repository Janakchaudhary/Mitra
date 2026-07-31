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
Milestone 5 is implemented: local book preparation, chapter structure, page knowledge, and concept extraction plumbing behind `AiGateway`. `MockAiGateway` intentionally produces only development analysis and leaves book-derived concepts disabled for child practice.

Before starting another milestone, verify Milestone 5 is green in GitHub Actions and on a physical Android device.

## Next scope — Milestone 5 only
Connect a real parent-configured AI provider behind `AiGateway` for:
- table-of-contents understanding from selected PDF pages
- chapter/page understanding
- grounded Gujarati tutor turns
- structured output validation and fallback
- enabling only valid analyzed book concepts for practice

Do not let the provider assign mastery or select curriculum. Never embed the provider secret in source or BuildConfig.


## Milestone 5 AI rules

- Keep `AiGateway` provider-neutral.
- Never compile API keys into the APK.
- Child transcripts/answers remain local in Milestone 5.
- Remote AI may analyze parent-selected textbook pages and generate grounded numeric question sets.
- Never make `practiceReady=false` concepts eligible for child practice.
