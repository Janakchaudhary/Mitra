## Milestone 16 — Resilient Study Talk and Offline Local AI

- Cloudflare chat accepts more Workers AI response envelopes and falls back to locally prepared textbook extraction instead of exposing parser errors to the child.
- A third **Offline Local** provider works without an API; a parent can optionally import a compatible `.litertlm` model for more natural on-device replies.
- Vague follow-ups reuse the previous child question when retrieving prepared textbook pages.
- The carry/borrow entry is vertically positioned directly above the `દશક` field.

# Mitra Android

**Current milestone: 16 (v0.16.0)** — resilient Cloudflare Study Talk, optional on-device LiteRT-LM, grounded offline textbook answers, guided maths, spaced review, offline question banks, and bounded hands-free conversation.

મિત્ર is a local-first Android learning companion for one Standard 2 Gujarati-medium child.

## Current capabilities

- Parent PIN and separate child/parent areas.
- Private PDF import, duplicate detection, chapter setup, parent-reviewed preparation, and local PDF viewing.
- OpenAI remains the default optional remote provider, Cloudflare Workers AI is the secondary cloud option, and Offline Local is the third provider for prepared-book chat without an API.
- Built-in Standard 2 maths, Gujarati, English, spelling, and tables remain deterministic and usable without a remote AI provider.
- Question fingerprints and recent-history suppression reduce repeated questions.
- Two-digit addition/subtraction with and without carrying/borrowing.
- Finger-writing rough-work notebook plus aligned tens/carry-or-borrow/ones fields.
- Local mistake classification and step-specific Gujarati hints.
- Spaced review scheduling at increasing intervals.
- Offline book-derived question banks generated during chapter preparation.
- Mixed activities: numeric, multiple choice, spelling, reading, vocabulary, stories, riddles, book-look, physical missions, drawing, and Teach-Mitra.
- Parent-selected practice for an exact built-in or prepared-book concept.
- Parent progress dashboard and weekly report.
- Backup and restore for local books, prepared data, progress, question banks, and non-secret learning settings.
- Configurable daily/session limits and automatic parent relocking.
- Stable GitHub Actions signing for update installs.
- Purposeful mascot, activity, progress, listening, thinking, and success animations without streaks or infinite reward loops.

## Mitra sathe vaat kariye

The child can use a bounded, turn-based hands-free conversation:

1. child speaks
2. Mitra processes the utterance
3. Mitra speaks its reply
4. listening begins again when hands-free mode is enabled

This is not unrestricted full-duplex audio. It retains microphone permission checks, a stop command, and the configured learning-time limit.

Study Talk has two answer paths:

- **Local Standard 2 maths:** addition, subtraction, carrying, borrowing, multiplication, tables 2–10, before/after, greater/smaller, Gujarati digits, and common Gujarati number words.
- **Prepared textbook grounding:** other study questions retrieve relevant locally prepared pages. OpenAI/Cloudflare may phrase the grounded answer, while Offline Local uses on-device extraction or an imported LiteRT-LM model. Missing textbook evidence is never replaced with web answers.

Questions about mobile games receive a short age-appropriate reminder that Mitra is already a learning game, prolonged mobile gaming can reduce study, sleep, physical play, and family time, and the phone should rest after the short session.

## Book preparation

1. Parent adds a PDF.
2. Parent selects contents/index pages or creates chapters manually.
3. Parent reviews and saves chapter ranges.
4. Parent presses **Prepare** for one chapter.
5. Mitra renders only that chapter's pages in small batches and asks the configured provider for structured page knowledge and concepts.
6. Prepared concepts are stored locally.
7. A reusable offline activity bank is generated while the parent is already online.
8. Parent can enable/disable detected concepts and see each concept's cached-question count.

## Build with GitHub Actions

Complete the one-time signing setup in [`SIGNING_SETUP.md`](SIGNING_SETUP.md). Then push to `main` and run **Android build**.

The workflow runs:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

Download the signed `mitra-update-apk` artifact. Once the stable-signed baseline is installed, future higher-`versionCode` artifacts can be installed over it without uninstalling.

## Upgrade

- `versionCode = 28`
- `versionName = 0.16.1`
- Room schema version `5`
- No new database migration is required. Existing books, prepared data, progress, review schedules, credentials, and sessions are preserved.

## Privacy boundary

Stored locally:

- PDFs and prepared page knowledge
- curriculum and offline question banks
- mastery, attempts, sessions, and settings

Not retained by default:

- raw microphone audio
- rough-work strokes
- free-form Study Talk history
- location, contacts, school, or advertising identifiers

API credentials and the parent PIN are excluded from exported backups. A parent-entered cloud API token inside a private mobile app remains a personal-development compromise and must never be committed to source control.

See [`MILESTONE16.md`](MILESTONE16.md) for Cloudflare resilience, Offline Local setup, and the carry-field correction; earlier milestone documents remain in the repository.


## Milestone 13

Purposeful child-friendly animations and visual polish.
