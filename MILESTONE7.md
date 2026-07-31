# Milestone 7 — Parent Progress & Adaptive Review

Version: 0.7.0 (versionCode 10)

Milestone 7 adds a parent-only, fully local learning progress dashboard.

## Added

- Parent `Learning progress` screen behind the existing PIN gate.
- Today's learning minutes and rolling seven-day minutes.
- Completed session count and assessed answer accuracy.
- Overall mastery across practiced concepts.
- Subject-level mastery, attempts, and concepts practiced.
- `Needs practice` concepts below 60% mastery.
- `Strong concepts` at or above 85% mastery.
- Recent session history with duration, correctness, and participation activity counts.
- Suggested next concept for practice.
- Prepared textbook concepts are preferred over built-in fallback curriculum in recommendations.
- All analytics are calculated locally from Room tables.
- Participation-only activities remain excluded from assessed accuracy.

## Storage / migration

No Room schema changes were required. No migration is needed from Milestone 6.

## Privacy

Opening the dashboard performs no AI/network request. It reads only local concepts, mastery, sessions, and attempts.
