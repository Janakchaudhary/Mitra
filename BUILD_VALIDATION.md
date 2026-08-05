# Mitra 0.23.0 Validation

This source package is Milestone 23 (`versionCode 44`, `versionName 0.23.0`, Room schema 6).

## Checks completed in the packaging environment

- Type-compiled `BookPreparationService` against project-compatible Android, Room, coroutine, AI, repository and DAO stubs after adding transactional chapter commits.
- Type-compiled `ParentQuizService` with the focused chapter selector, Room question path and legacy fallback.
- Ran pure Kotlin logic checks for:
  - exact-fact adaptive prioritization;
  - unique 20-question chapter selection across pages and activity types;
  - Gujarati FTS query normalization;
  - accepting a valid English article variation;
  - rejecting incorrect English word order.
- Parsed the changed Compose screens together and found no Kotlin syntax/structural diagnostics.
- Ran a SQLite schema smoke test for migration-5-to-6 table/index creation, FTS4 backfill and Gujarati prefix matching.
- Scanned the project for unresolved merge markers and stale delegated-state imports.
- Verified `versionCode 44`, `versionName 0.23.0`, Room schema 6 and the explicit `5 → 6` migration.

## Android build note

A complete `testDebugUnitTest`, KSP, Lint and `assembleDebug` run was not executed here because this packaging environment has no Android SDK or Gradle executable. The included GitHub Actions workflow installs Gradle 8.13 and Android SDK 35, then runs unit tests, Lint and the debug APK build.

## Upgrade behavior

- Existing schema-5 databases migrate to schema 6 without destructive reset.
- Existing PDFs, chapters, page knowledge, mastery, attempts and credentials are preserved.
- Existing file-based offline question banks remain usable and are progressively superseded by Room question records after a chapter is prepared or a `.mitrabook` package is imported.
