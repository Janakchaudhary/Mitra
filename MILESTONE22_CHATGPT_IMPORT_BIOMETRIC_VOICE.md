# Mitra 0.22.0 — ChatGPT Prepared Books, Biometric-First Parent Access and Longer Learning

## Prepared books from ChatGPT

The Parent **My books** screen now contains a **Prepare with ChatGPT** card.

1. Tap **Copy prompt**, then tap **Open ChatGPT**.
2. Upload the textbook PDF in ChatGPT and paste the prompt.
3. Ask ChatGPT to create one `.mitrabook` UTF-8 JSON file.
4. Tap **Import prepared book** in Mitra.

The schema imports physical PDF chapter ranges, page knowledge, Gujarati vocabulary, concepts and offline questions. Packages are validated before Room data is changed. If `sourcePdfSha256` matches an existing PDF, the prepared data is attached to that book. When a hash is unavailable, an exact title/subject/standard/page-count match can attach it. Otherwise Mitra creates a ready-to-study package-only book.

Package-only books support Mitra ne Puchiye, practice and parent tests without storing the source PDF. A reusable prompt is included at `MITRA_CHATGPT_PREPARATION_PROMPT.txt`, and a sample package is included at `app/src/main/assets/mitra_prepared_book_example.mitrabook`.

## Parent unlock

- Added AndroidX BiometricPrompt.
- When a strong biometric is enrolled, the system biometric prompt opens first.
- The first visible action is fingerprint/biometric unlock.
- Device PIN/pattern/password remains available through the system prompt or fallback.
- Mitra parent PIN remains available and still auto-verifies after the final digit.

## Learning limits

- Default daily learning allowance: **180 minutes (3 hours)**.
- Default session length: **60 minutes**.
- Settings choices now include sessions up to 60 minutes and daily allowances up to 180 minutes.
- Existing pre-Milestone-22 settings are upgraded once to the new requested defaults.

## Voice

- Added **Cartoon Adventure**, an original energetic Mitra voice profile.
- Mitra selects the highest-quality installed local voice for Gujarati/English where possible.
- Local voices are preferred for lower latency.
- Pitch, speed and expressive punctuation are tuned for a livelier child-friendly delivery.
- The app does not imitate or clone any specific copyrighted cartoon/anime character voice.
- Actual naturalness still depends on the TTS voices installed on the phone.

## Version

- `versionName = 0.22.0`
- `versionCode = 42`
- Room schema remains version 5; no database migration is required.
