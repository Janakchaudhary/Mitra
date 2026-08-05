# Mitra Android

## Milestone 23 — Focused Learning and Faster Prepared Books

**Current version: 0.23.0 (`versionCode 44`)**  
**Room schema: 6**

Mitra is a local-first Android learning companion for a Standard 2 Gujarati-medium child. It combines prepared-textbook Q&A, two-way voice practice, adaptive skill sessions, parent-created tests and visual teaching.

## Focused chapter tests

The Parent **Test Builder** now uses a guided flow:

1. choose the test type;
2. choose a READY book;
3. choose exactly one READY chapter;
4. choose 20 or 25 questions;
5. create a one-mark-per-question child test.

Prepared-book tests never mix chapters. The UI shows the number of unique approved questions available in each chapter and disables a test length when that chapter does not contain enough distinct questions.

## Prepared-book performance

Milestone 23 adds persistent and indexed book data:

- exact Gujarati vocabulary records;
- reusable prepared questions in Room;
- cached embedded PDF/OCR text per physical page;
- Room FTS4 textbook search;
- persistent WorkManager preparation jobs;
- Room-backed parent quiz plans and ordered questions.

Book Q&A now checks exact vocabulary first, then indexed textbook pages, then a bounded OCR-tolerant fallback. It no longer scans every prepared page for every child question.

## Background preparation

Chapter preparation runs as persistent WorkManager work with progress shown in My Books. “Prepare all” processes chapters sequentially to control memory use. Up to 100 varied chapter questions are generated in bounded chapter-level batches and stored for instant offline tests.

Page knowledge, concepts, vocabulary, prepared questions and READY status are committed together. A failed re-prepare keeps the previous complete READY lesson.

## ChatGPT prepared-book import

The Parent **My Books** screen supports:

1. add a PDF and prepare it in Mitra;
2. prepare the textbook in ChatGPT and import a `.mitrabook`, JSON or ZIP package.

Use **Copy prompt → Open ChatGPT → upload PDF → download `.mitrabook` → Import prepared book**. The app validates physical PDF page ranges, chapters, vocabulary, page knowledge, concepts and questions before writing them to Room.

Reference files:

- `MITRA_CHATGPT_PREPARATION_PROMPT.txt`
- `app/src/main/assets/mitra_prepared_book_example.mitrabook`

## Better teaching

### મિત્રને પૂછીએ

- child-led prepared-book questions;
- Mitra-led tables, number-neighbour, spelling and book practice;
- voice or typing;
- dynamic praise, one guided retry and a simple correction;
- automatic next question after spoken feedback;
- Room-first prepared question retrieval.

### Adaptive sessions

The planner uses exact question/fact fingerprints from recent attempts. Incorrect or skipped facts return sooner, recently correct facts are spaced out, and repeated activity types are reduced.

### Activities

- **રંગોની મજા:** 20 rounds, Gujarati/English/spelling prompts and visibly coloured object tiles.
- **English Sentence Builder:** picture prompts, speech, shuffled help words, article variation support and word-order feedback.
- **કૌશલ્ય રમત:** 20/25-question adaptive sessions and compact guided arithmetic.
- **પડછાયો:** animated narration, child prediction and draggable torch experiment.

## Parent experience

- biometric/fingerprint-first unlock when enrolled;
- device credential and Mitra PIN fallbacks;
- automatic PIN verification after the final digit;
- 60-minute default session length;
- 180-minute daily maximum;
- focused Book → Chapter test creation.

## Voice

The original **Cartoon Adventure** profile selects an installed Gujarati/English TTS voice and adjusts pitch/rate for an energetic Mitra style. It does not copy a specific cartoon or anime character. Voice quality depends on the TTS engine and language voices installed on the phone.

## Upgrade

- `versionCode = 44`
- `versionName = 0.23.0`
- Room schema `6`
- Explicit migration `5 → 6`
- No destructive migration
- Existing books, prepared pages, progress, mastery and credentials are preserved
- Legacy file question banks remain usable as a compatibility fallback

## Build

Complete signing setup in [`SIGNING_SETUP.md`](SIGNING_SETUP.md), push to `main`, and run **Android build**. GitHub Actions installs Gradle and Android SDK 35, then runs unit tests, Lint and `assembleDebug`.

See [`MILESTONE23_FOCUSED_LEARNING.md`](MILESTONE23_FOCUSED_LEARNING.md), [`BUILD_VALIDATION.md`](BUILD_VALIDATION.md), [`ARCHITECTURE.md`](ARCHITECTURE.md), [`SIGNING_SETUP.md`](SIGNING_SETUP.md) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
