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
- Learning engine controls curriculum; AI will later control only conversational presentation.

## Development process
Implement one milestone at a time. For every milestone:
1. Keep domain/business logic outside Compose functions.
2. Add/update tests.
3. Run `gradle testDebugUnitTest`.
4. Run `gradle lintDebug`.
5. Run `gradle assembleDebug`.
6. Do not disable tests or lint just to make builds pass.

## Current scope
Milestone 1 only: parent PIN, child/parent navigation, PDF import, private copy, SHA-256 duplicate detection, Room book library, PDF viewer, delete book.
Do not implement AI or voice yet.
