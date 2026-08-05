# Mitra Android

## Milestone 22 — Prepared Book Import and Smarter Parent Experience

**Current version: 0.22.0 (`versionCode 42`)**

Mitra is a local-first Android learning companion for a Standard 2 Gujarati-medium child. It combines prepared-textbook Q&A, two-way voice practice, guided mathematics, language activities, parent-created tests and visual teaching.

## ChatGPT prepared-book import

The Parent **My books** screen now offers two book paths:

1. Add a PDF and prepare it inside Mitra.
2. Prepare the textbook in ChatGPT and import a `.mitrabook` or `.json` package.

Tap **Copy prompt**, tap **Open ChatGPT**, upload the textbook, paste the prompt and download the generated `.mitrabook` JSON file. Mitra validates and imports:

- physical PDF chapter ranges;
- page summaries and visible textbook text;
- Gujarati vocabulary meanings and examples;
- learning concepts and outcomes;
- reusable offline questions for practice and tests.

If the package contains the original PDF SHA-256, Mitra attaches it to the matching local PDF. It can also match one exact title/subject/standard/page-count book when the hash is unavailable. A package can be used without a local PDF; its knowledge remains available to **મિત્રને પૂછીએ**, quizzes and tests.

A reusable prompt and sample package are included at:

- `MITRA_CHATGPT_PREPARATION_PROMPT.txt`
- `app/src/main/assets/mitra_prepared_book_example.mitrabook`

## Main child experience

### “મિત્રને પૂછીએ”

- The child can ask questions from locally prepared or imported books.
- Mitra can ask tables, before/after numbers, spelling, mixed or prepared-book questions.
- Conversation can begin from either side.
- Correct answers receive varied spoken appreciation and move automatically to the next question.
- A wrong answer receives a hint and then a simple explanation.
- Voice and typing remain available.

### Learning games

- **રંગોની મજા** colour identification and spelling.
- **English Sentence Builder** with picture prompts, speech and help words.
- **કૌશલ્ય રમત** 20/25-question skill sessions.
- Compact guided arithmetic with carry/borrow above the tens column.
- Animated and narrated **પડછાયો** teaching sequence.

## Parent area

- Fingerprint/biometric unlock is the first option when enrolled.
- The system device credential and Mitra parent PIN remain fallbacks.
- Parent PIN auto-verifies after the final stored digit.
- Parent Test Builder creates 20/25-mark tests from skills or prepared books.
- Default learning time is **3 hours per day** with **60-minute sessions**.

## Voice

The default **Cartoon Adventure** profile is an original energetic Mitra voice. It chooses the best installed local Gujarati/English TTS voice where possible and tunes pitch/rate for a livelier delivery. It does not copy a specific cartoon or anime character. Voice quality depends on the speech engine and language voices installed on the phone.

## Offline PDF preparation

Mitra still supports fully local preparation:

1. embedded PDF text extraction;
2. Gujarati/English OCR for scanned pages;
3. physical-page chapter mapping;
4. local page/concept/question caching;
5. optional LiteRT-LM structuring with deterministic fallback.

## Build with GitHub Actions

Complete the signing setup in [`SIGNING_SETUP.md`](SIGNING_SETUP.md), push to `main`, and run **Android build**. The workflow runs unit tests, Android Lint and `assembleDebug`.

## Upgrade

- `versionCode = 42`
- `versionName = 0.22.0`
- Room schema version `5`
- No Room migration is required.
- Existing PDFs, prepared knowledge, question banks, progress and credentials are preserved.
- Pre-Milestone-22 learning limits are upgraded to the requested 60-minute session and 180-minute daily defaults.

See [`MILESTONE22_CHATGPT_IMPORT_BIOMETRIC_VOICE.md`](MILESTONE22_CHATGPT_IMPORT_BIOMETRIC_VOICE.md), [`ARCHITECTURE.md`](ARCHITECTURE.md), [`SIGNING_SETUP.md`](SIGNING_SETUP.md) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
