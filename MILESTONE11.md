# Milestone 11 — Varied Maths + Rough Work

## Added

- Recent-question fingerprints stored locally in Room.
- Exact questions are suppressed across recent sessions when alternatives exist.
- Questions are unique inside each session.
- Skill mode is now a mixed challenge instead of six repetitions of one concept.
- Every skill session includes two-digit addition and an addition-with-carry challenge.
- Two-digit arithmetic is generated dynamically rather than from five fixed examples.
- Direct sums, vertical-arrangement prompts and word problems rotate automatically.
- Finger-writing rough-work board for supported arithmetic questions.
- Undo and clear controls for rough work.
- Refreshed child session UI with clearer cards, timer chip and larger controls.

## Database

Room version 4 adds `attempts.questionFingerprint` and an index. Migration 3 → 4 preserves all existing books, progress and sessions.

## Version

- versionCode: 18
- versionName: 0.11.0
