# Mitra Android

## Milestone 20 — Smart Tutor

**Current version: 0.20.0 (`versionCode 38`)**

Mitra is a local-first Android learning companion for one Standard 2 Gujarati-medium child. It combines prepared-textbook Q&A, voice practice, guided maths, language games, parent-created tests and animated visual teaching without requiring a backend.

## Main child experience

### “મિત્રને પૂછીએ”

- The child can ask questions from any locally prepared book.
- Mitra can ask tables, before/after number, spelling, mixed or prepared-book questions.
- Conversation can start from either side: the child may ask freely, or Mitra may start a 20/25-question game.
- During a Mitra-led quiz the child can ask a textbook question; Mitra answers it and resumes the current challenge.
- Correct answers receive varied spoken appreciation and move automatically to the next question.
- A wrong answer gets one useful hint; the second wrong attempt explains the correct method and continues.
- Gujarati recognition uses `gu-IN`; English spelling/sentence activities use `en-IN`.
- Voice and typing are always available as alternatives.

### Learning games

- **રંગોની મજા:** pick a colour, say its Gujarati name, say its English name and spell it.
- **English Sentence Builder:** look at a picture/emoji, speak or type a full sentence, and use optional grammar/word help.
- **કૌશલ્ય રમત:** 20-question Standard 2 mixed skill sessions, with support for 25-question generation.
- **Guided arithmetic:** ones/tens result fields appear in the sum itself; carry/borrow is above the tens column. The optional finger-writing pad is collapsed to keep the arithmetic page compact.
- **પડછાયો visual lesson:** six narrated animated steps show how light, object position and distance change a shadow.

## Parent area

- Parent PIN with automatic verification after the last stored PIN digit.
- Android phone-lock/device-credential confirmation as an alternative where available.
- Parent Test Builder for 20- or 25-mark tests from:
  - any selected built-in Standard 2 skill (addition, subtraction, tables, Gujarati or English)
  - prepared books
  - tables
  - before/after numbers
  - English spelling
  - mixed practice
- Every child-test question carries one mark and accepts voice or typed answers.
- Book import, chapter setup, preparation, progress, exact-skill practice, backup/restore, AI settings and learning-time limits remain available.

## Offline book preparation

1. Parent imports a private PDF.
2. Mitra reads embedded PDF text first and uses bundled Gujarati/English OCR for scanned pages.
3. Parent detects or manually sets chapter ranges.
4. Printed textbook page numbers are mapped to physical PDF pages when front matter is present.
5. Before preparing a generic stale range, Mitra can scan early contents pages and repair chapter boundaries locally.
6. Four pages are processed at a time to bound memory.
7. An optional imported `.litertlm` model structures page summaries/concepts; a deterministic local fallback still works without it.
8. Prepared page text, concepts and reusable question banks are stored locally.

Prepared-book answering filters out unrelated pages and prefers exact/stem/OCR-near matches. It does not silently replace missing textbook evidence with web answers.

## Child-mode containment

Mitra requests Android lock-task/screen-pinning when child mode opens and releases it after parent authentication. On a normal phone Android may still allow the owner to exit screen pinning using the system gesture and device credential. Fully enforced kiosk behavior requires device-owner provisioning.

## Build with GitHub Actions

Complete the one-time signing setup in [`SIGNING_SETUP.md`](SIGNING_SETUP.md), push to `main`, and run **Android build**. The workflow runs unit tests, lint and `assembleDebug`, then publishes the signed update APK.

## Upgrade

- `versionCode = 38`
- `versionName = 0.20.0`
- Room schema version `5`
- No database migration is required from Milestone 19.
- Existing PDFs, prepared knowledge, question banks, progress, review schedules, credentials and settings are preserved.

## Privacy boundary

Stored locally:

- imported PDFs and prepared page knowledge
- curriculum and offline question banks
- mastery, attempts, sessions and settings
- optional parent-imported LiteRT-LM model
- active parent-created test

Not retained by default:

- raw microphone audio
- rough-work strokes
- temporary Study Talk turns after the screen closes
- location, contacts, school or advertising identifiers

API credentials and the parent PIN are excluded from backups. The local model is also excluded from normal backups.

See [`MILESTONE20.md`](MILESTONE20.md), [`ARCHITECTURE.md`](ARCHITECTURE.md), [`SIGNING_SETUP.md`](SIGNING_SETUP.md) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
