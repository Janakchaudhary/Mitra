# Codex instructions for Mitra

Read `ARCHITECTURE.md` before architectural changes.

## Fixed constraints

- Native Android, Kotlin, Jetpack Compose.
- Local-first, one child, one APK.
- Room + DataStore + app-private files only.
- No Firebase, Supabase, PostgreSQL, account system, backend database, ads, browser, YouTube, feed, or streak mechanics.
- Imported PDFs are copied into `files/books/{bookId}/source.pdf`.
- Parent configuration is PIN protected.
- Never embed API or signing secrets in source/BuildConfig.
- `LearningEngine` owns evaluation/mastery/review decisions.
- `AiGateway` may present/analyze grounded material but must never write mastery.
- Built-in Standard 2 drills must remain local/deterministic and work without a remote provider.
- Text input must remain available if voice is unavailable.
- Raw audio, rough-work strokes, and Study Talk history must not be persisted.

## Current scope — Milestone 12

Implemented:

- exact/recent question suppression and mixed question forms
- guided two-digit addition/subtraction with carry/borrow fields
- local mistake classification and targeted retry
- spaced review using Room schema v5
- offline prepared-book question bank
- parent concept enable/disable and exact skill selection
- weekly parent report
- local backup/restore excluding secrets/PIN
- speech confirmation before answer submission
- bounded turn-based hands-free Study Talk
- local Standard 2 maths explanations in Study Talk
- mobile-game balance guidance

## Milestone 12 invariants

- Keep `questionFingerprint` and migrations `3 → 4` and `4 → 5`.
- Never persist rough-work strokes.
- Participation-only activities never improve mastery.
- Do not reveal a spelling target on screen before submission.
- Built-in skill practice must not depend on OpenAI or Cloudflare.
- Study Talk must not add web search.
- Prepared-book answers must remain grounded in `StudyContextService` sources.
- Hands-free mode must respect stop commands and learning-time limits.
- Exact parent-selected practice must use the requested concept.
- Exported backups must exclude API credentials, parent PIN, and signing files.

## Development process

For each change:

1. Keep domain/business logic outside Composables.
2. Add or update tests.
3. Run `gradle testDebugUnitTest`.
4. Run `gradle lintDebug`.
5. Run `gradle assembleDebug`.
6. Never disable tests/lint to get green CI.
7. Add explicit Room migrations for schema changes.
8. Preserve the stable signing workflow and increment `versionCode` for every installable release.
