# Milestone 23 — Focused Learning Architecture

**Version:** 0.23.0 (`versionCode 44`)  
**Room schema:** 6

Milestone 23 restructures the highest-traffic learning paths so prepared textbooks remain fast as the library grows and a parent can teach one book chapter at a time.

## Focused Test Builder

The parent test flow is now a guided sequence:

1. choose **પુસ્તકનો પાઠ**;
2. choose one READY book;
3. choose one READY chapter;
4. choose 20 or 25 questions (one mark each);
5. review the book/chapter summary and create the child test.

Prepared-book tests never mix questions from another chapter. The builder shows the number of unique approved questions in each chapter and disables 20/25 choices when the selected chapter does not have enough. `FocusedChapterQuestionSelector` balances question type, page and concept while preventing duplicate fingerprints.

## Room schema 6

The migration adds:

- `vocabulary` — exact word meanings, explanations, examples and physical PDF pages;
- `prepared_questions` — approved reusable questions with source pages and usage metadata;
- `raw_page_text` — cached embedded-text/OCR output by PDF source and physical page;
- `preparation_jobs` — persistent WorkManager status and progress;
- `page_knowledge_fts` — Room FTS4 index for fast textbook retrieval;
- `parent_quiz_plans` and `parent_quiz_questions` — persistent focused tests and history-safe question ordering.

Migration `5 → 6` is explicit. Destructive migration is not enabled. Existing file-based question banks remain readable as a compatibility fallback and are replaced by Room data on the next import or preparation.

## Faster, safer book preparation

- Embedded PDF text/OCR is cached once per physical page.
- Chapter preparation runs as foreground WorkManager work and survives navigation/process recreation.
- “Prepare all” is a sequential unique work chain to bound memory and OCR/model pressure.
- Question generation is bounded to four chapter-level batches (up to 100 varied questions), rather than one request for every concept.
- New page knowledge, concepts, vocabulary, questions and READY status are committed in one Room transaction.
- A failed re-prepare keeps the previous READY snapshot.
- Replacing an active preparation job cancels its stale job state before queuing the new one.

## Faster prepared-book teaching

`StudyContextService` now retrieves in this order:

1. exact normalized vocabulary;
2. FTS4 textbook match;
3. bounded OCR-tolerant fallback.

It no longer scans every stored page for every child question. Meanings such as `દંગોરો` are read directly from structured vocabulary and include the physical PDF page.

Prepared question selection is Room-first in the learning engine, voice tutor and parent tests. Legacy JSON banks remain fallback-only.

## Adaptive learning

`AdaptiveSessionPlanner` uses exact question/fact fingerprints from recent attempts. Incorrect and skipped facts return sooner, recently correct facts are delayed, and repeated activity types are limited. Session generation keeps the pre-session history separate from the newly generated candidate pool so every fresh candidate is not mistakenly penalized as “recent.”

## Teaching UI improvements

- Sentence Builder accepts safe article variations, checks important grammar/order, shuffles help words and reveals stronger help after repeated errors.
- Colour activity uses a visibly tinted object tile, keeps the answer hidden until appropriate and advances after TTS completion.
- Shadow lesson asks for a direction prediction, explains the result and provides a draggable torch experiment.
- Shared `speakAndAwait` sequencing advances after TTS completion with a safe fallback rather than fixed delays.
- Child focused tests show book → chapter, source page labels and one-mark-per-question progress.

## Validation boundary

Targeted Kotlin type checks passed for the book preparation service and focused parent test service. Pure Kotlin checks passed for adaptive selection, focused chapter selection, FTS query generation and grammar-aware sentence evaluation. A SQLite smoke test created the schema-6 tables, backfilled FTS and matched Gujarati textbook text.

A full Android Gradle/KSP/Lint/APK run was not executed in the packaging environment because it has no Android SDK or Gradle executable. The included GitHub Actions workflow performs those checks.
