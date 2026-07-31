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
- Built-in Standard 2 skill drills must remain local/deterministic and must not require a remote provider.
- Voice input/output must remain replaceable behind `SpeechInput` and `SpeechOutput`.
- Text answer entry must continue working when voice is unavailable or permission is denied.
- Gujarati voice defaults to `gu-IN`; English spelling/read-aloud may use `en-IN` where the activity requests it.

## Development process
For every milestone or fix:
1. Keep domain/business logic outside Compose functions.
2. Add/update tests.
3. Run `gradle testDebugUnitTest`.
4. Run `gradle lintDebug`.
5. Run `gradle assembleDebug`.
6. Do not disable tests or lint just to make builds pass.
7. Preserve Room data with explicit migrations; never use destructive migration in release behavior.

## Current scope — Milestone 9
Milestone 9 is implemented in this source. It adds the offline Standard 2 skill engine:
- two-digit addition/subtraction with separate carry/borrow mastery,
- missing numbers, comparison and word problems,
- multiplication meaning and tables 2–10 as separate concepts,
- Gujarati word recognition, spelling, missing letters, read aloud, sentence completion, meaning and singular/plural,
- English word recognition, spelling, missing letters, read aloud and sentence completion,
- dedicated child `કૌશલ્ય રમત` mode,
- per-activity speech/recognition language tags,
- parent progress listing every built-in Standard 2 skill.

## Milestone 9 guardrails
- Do not collapse all multiplication tables into one mastery record.
- Do not collapse carrying and non-carrying addition into one mastery record.
- Do not collapse borrowing and non-borrowing subtraction into one mastery record.
- Spelling dictation must not display the target word before the answer is submitted.
- Participation-only activities must never improve mastery.
- Book-derived concepts may use remote AI; built-in skill practice must remain usable offline.
- Keep session and daily learning limits enforced for both normal and skill-only sessions.
