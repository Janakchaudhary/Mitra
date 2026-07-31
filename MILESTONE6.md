# Milestone 6 — Rich child-safe learning activities

Version: **0.6.0** (`versionCode 9`)

Milestone 6 expands the learning session beyond integer-only questions while keeping curriculum/mastery decisions local.

## New activity types

- QUESTION
- MULTIPLE_CHOICE
- RIDDLE
- STORY
- BOOK_LOOK
- READING
- VOCABULARY
- PHYSICAL_MISSION
- DRAW
- TEACH_MITRA
- RECAP

## Evaluation modes

- `NUMERIC` — evaluated locally with Gujarati/English digit normalization.
- `MULTIPLE_CHOICE` — evaluated locally against an accepted option.
- `SHORT_TEXT` — evaluated locally against a small explicit accepted-answer set.
- `KEYWORD` — evaluated locally only when the prepared activity declares clear keywords.
- `PARTICIPATION` — recorded as `UNKNOWN`; it does not raise/lower mastery or assessed-attempt counts.

## Hints

Activities may include one short hint. The UI exposes **સંકેત**. Assessed answers carry `hintsUsed` into the existing mastery policy, so a correct answer after hints gains less mastery than an unaided correct answer.

## Physical safety boundary

Remote AI text is never used verbatim as a physical mission. `ActivitySafetyPolicy` replaces `PHYSICAL_MISSION` content with a local allowlist of stationary/nearby activities such as pencils, books, safe objects, or clapping. Drawing instructions are also replaced with a local paper-and-pencil prompt.

`ActivityPlanPolicy` ensures a session of four or more activities includes an off-screen activity even if the AI provider omitted one.

## Book-derived language concepts

New chapter analysis can mark normal Standard 2 reading/vocabulary/story concepts `practiceReady=true`, because Milestone 6 can now represent non-numeric learning safely. Chapters prepared under Milestone 5 may still have language concepts marked false; use **Prepare again** on those chapters to regenerate concept readiness.

## Child data boundary

Child microphone recordings, transcripts, answers, mastery, and session history are still not sent to the remote model. Remote AI is used to analyze parent-selected textbook pages and generate a bounded activity plan from cached textbook knowledge. Evaluation remains local.

## Database

No Room schema change is required. Existing books, chapters, page knowledge, concepts, mastery, sessions, attempts, and PIN survive an in-place upgrade from 0.5.1.
