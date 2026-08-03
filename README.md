# Mitra Android

## Milestone 19 — “મિત્રને પૂછીએ” voice tutor

**Current version: 0.19.1 (`versionCode 36`)**

મિત્ર is a local-first Android learning companion for one Standard 2 Gujarati-medium child. Milestone 19 adds a two-way voice tutor that answers prepared-book questions and runs spoken practice with adaptive feedback. Milestone 18 offline PDF/OCR preparation remains included.

### 0.19.1 voice reliability patch

- Uses the normal Android speech recognizer first instead of forcing an on-device Gujarati model.
- Stops the continuous “અવાજ સમજવામાં સમસ્યા આવી” restart loop.
- Automatically retries only one temporary BUSY/CLIENT failure.
- Shows specific guidance for missing Gujarati/English speech languages and modern Android error codes.



## What “મિત્રને પૂછીએ” now does

- Answers child questions from locally prepared textbook pages.
- Answers ઘડિયા/પહાડા, પહેલાંની સંખ્યા, પછીની સંખ્યા, arithmetic, and common English spelling requests locally.
- Offers one-tap voice practice for prepared-book questions, tables, number neighbors, spelling, or mixed practice.
- Speaks each question, switches speech recognition to Gujarati or English as needed, and evaluates the child’s spoken reply.
- Gives varied praise for correct answers and tracks the current correct streak.
- Gives one guided retry after a wrong answer; after the second attempt it explains the correct method and continues with a new question.
- Prefers Android on-device speech recognition and requests offline recognition where the device supports it.
- Does not store raw microphone audio or free-form voice transcripts after the screen closes.

## What Offline Local now does

- Reads selectable text directly from a PDF on the phone.
- Falls back to bundled Gujarati + English OCR for scanned/image-only pages.
- Detects chapters from parent-selected contents/index pages.
- Prepares one chapter or every saved chapter in sequence.
- Uses a parent-imported `.litertlm` model to produce higher-quality Gujarati summaries and concepts.
- Uses a conservative rule-based fallback when no compatible local model is installed, so page knowledge can still be created.
- Stores prepared page text, summaries, concepts, and question banks locally.
- Keeps existing `READY` content usable when a re-prepare fails.

The local language model remains text-only. Page images are converted to text by the on-device PDF/OCR pipeline before being sent to LiteRT-LM.

## Current capabilities

- Parent PIN and separate child/parent areas.
- Private PDF import, duplicate detection, chapter setup, parent-reviewed preparation, and local PDF viewing.
- OpenAI, Cloudflare Workers AI, and Offline Local providers.
- Built-in Standard 2 maths, Gujarati, English, spelling, and tables without remote AI.
- Question fingerprints and recent-history suppression to reduce repetition.
- Two-digit addition/subtraction with carrying and borrowing.
- Finger-writing rough work and aligned place-value fields.
- Local mistake classification and step-specific Gujarati hints.
- Spaced review scheduling.
- Offline book-derived activity banks.
- Parent-selected practice for an exact built-in or prepared-book concept.
- Parent progress dashboard, weekly report, backup, and restore.
- Configurable daily/session limits and automatic parent relocking.
- Bounded turn-based voice conversation with spoken quizzes and adaptive correction.

## Offline book preparation flow

1. Parent imports a PDF.
2. Parent opens **Set up chapters** and marks one or more contents/index pages.
3. Offline Local first tries embedded PDF text. Scanned pages are rendered and recognized with Gujarati/English OCR.
4. Mitra detects likely chapter titles and start pages. The parent reviews and corrects them before saving.
5. Parent presses **Prepare** for one chapter or **Prepare all chapters**.
6. Pages are processed in bounded batches. Embedded text is preferred; OCR runs only where needed.
7. An imported LiteRT-LM model converts the extracted text into structured page summaries and concepts. Without a model, a local deterministic fallback creates conservative page knowledge and concepts.
8. Prepared data and reusable questions are stored locally for Study Talk and practice.

Printed page numbers in a textbook can differ from PDF page numbers because of cover/front-matter pages. The parent should review detected ranges before saving.

## Study Talk

The child can use a bounded, turn-based conversation:

1. child speaks
2. Mitra processes the utterance
3. Mitra speaks its reply
4. listening begins again when hands-free mode is enabled

Study Talk has two grounded paths:

- **Local Standard 2 maths:** deterministic support for addition, subtraction, carrying, borrowing, multiplication, tables 1–20, before/after, comparison, Gujarati digits, and common Gujarati number words.
- **Prepared textbook grounding:** questions retrieve relevant locally prepared pages. Offline Local answers through the imported LiteRT-LM model or a deterministic extractive fallback. Missing textbook evidence is not replaced with web answers.

## Build with GitHub Actions

Complete the one-time signing setup in [`SIGNING_SETUP.md`](SIGNING_SETUP.md), push to `main`, and run **Android build**.

The workflow runs:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

Download the signed `mitra-update-apk` artifact. A higher `versionCode` signed with the same key can update the installed app without uninstalling it.

## Upgrade

- `versionCode = 36`
- `versionName = 0.19.1`
- Room schema version `5`
- No database migration is required from Milestone 17.
- Existing books, prepared data, progress, review schedules, credentials, and sessions are preserved.
- The APK is larger because Gujarati and English OCR language data are bundled.

## Privacy boundary

Stored locally:

- PDFs and extracted/prepared page knowledge
- curriculum and offline question banks
- mastery, attempts, sessions, and settings
- optional parent-imported LiteRT-LM model

Not retained by default:

- raw microphone audio
- rough-work strokes
- free-form Study Talk history
- location, contacts, school, or advertising identifiers

Offline preparation does not upload textbook pages or OCR text. API credentials and the parent PIN are excluded from exported backups. The imported local model is also excluded from normal backups.

See [`MILESTONE19.md`](MILESTONE19.md) for the voice tutor implementation, [`MILESTONE18.md`](MILESTONE18.md) for offline preparation, [`ARCHITECTURE.md`](ARCHITECTURE.md) for runtime boundaries, and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for the offline PDF/OCR components.


## 0.18.1 OCR compilation fix

Corrected the Tesseract4Android `TessBaseAPI` import to `com.googlecode.tesseract.android.TessBaseAPI`.
