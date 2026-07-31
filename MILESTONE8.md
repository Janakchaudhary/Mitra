# Milestone 8 — Polish & Hardening

Milestone 8 adds the production-style guardrails needed for the personal child app while keeping all core data local.

## Added

- Configurable session limit (15/20/30 minutes).
- Configurable daily allowance (15/30/45/60 minutes).
- Child home displays today's usage and remaining time.
- Learning sessions enforce both daily and per-session limits inside the ViewModel.
- Parent access automatically relocks after a configurable timeout and after a short background grace period (so the Android PDF picker does not instantly discard parent context).
- Offline indicator on child home.
- If remote book question generation fails, the engine can fall back to an eligible built-in local concept without pretending it came from the book.
- Parent data controls:
  - reset learning progress,
  - remove prepared analysis while preserving PDF/chapter structure,
  - reset entire app.
- Full reset removes local books, Room data, AI credential, settings, and PIN.
- Child-facing controls have larger touch targets and clearer Gujarati time-limit messaging.
- No Room migration is required; settings live in DataStore and existing database tables are reused.

## Version

`0.8.0` / versionCode `11`.
