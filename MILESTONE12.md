# Milestone 12 — Smart Tutor, Guided Maths and Continuous Study Talk

Version: `0.12.0` (`versionCode 20`)

## Guided two-digit maths

- Two-digit addition and subtraction show the existing freehand rough-work notebook.
- A new guided column area checks the ones, carry/borrow, and tens steps separately.
- Step-specific Gujarati feedback explains the exact place that needs another attempt.
- Numeric mistakes are classified locally, including forgotten carry, forgotten borrow, reversed digits, ones/tens mistakes, and multiplication-table mistakes.
- A retry is encouraged before the final explanation is shown.

## Spaced review

Mastery now stores:

- `nextReviewAt`
- `reviewIntervalDays`
- `consecutiveSuccesses`

Successful practice moves through review intervals of 1, 3, 7, 14, and 30 days. A mistake schedules the concept sooner. Due concepts are selected before unrelated new material.

Room schema is version 5 and includes a `4 → 5` migration. Existing books, progress, attempts, and question history are preserved.

## Offline textbook question bank

Preparing a chapter now also generates a small offline activity bank for each prepared concept. The bank is stored in the app's private files and can be used if the remote provider is unavailable.

The book detail screen shows the cached question count for each detected concept. Re-preparing a chapter replaces stale cached questions.

## Parent controls

- Start practice for an exact skill or prepared textbook concept.
- Enable or disable individual detected book concepts.
- Preview concept readiness and offline question counts.
- Weekly report with minutes, accuracy, most-practised concept, weak concept, and due-review count.

## Backup and restore

Parent settings can export and restore a local Mitra ZIP backup containing:

- Room database
- imported book files
- offline question banks
- non-secret learning settings and voice style

API credentials, parent PIN, signing keys, raw audio, and in-memory study conversations are excluded.

## Improved speech confirmation

Recognised speech is shown in the answer field first. The child confirms it with **જવાબ ચકાસો**, preventing a speech-recognition error from immediately changing mastery.

## Mitra sathe vaat kariye

Study Talk now supports a bounded hands-free, turn-based conversation:

1. child speaks
2. Mitra answers and speaks
3. after TTS finishes, listening starts again when hands-free mode is enabled

This is not unrestricted full-duplex audio. It preserves stop controls, microphone permission handling, and the configured learning-time limit.

The local responder can answer Standard 2 maths without a remote provider, including:

- two-number addition and subtraction
- detailed carrying and borrowing explanations
- multiplication and tables 2–10
- before/after and greater/smaller numbers
- Gujarati digits and common Gujarati number words

Prepared textbook questions still use grounded retrieval and the selected remote AI provider.

Questions about mobile games receive a brief age-appropriate response: Mitra explains that the child is already playing a learning game, that prolonged mobile gaming can reduce study, sleep, physical play, and family time, and suggests putting the phone down after the short learning session.
