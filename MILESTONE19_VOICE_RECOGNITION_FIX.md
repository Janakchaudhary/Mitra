# Mitra 0.19.1 — Voice recognition reliability fix

This patch fixes the repeated Gujarati message saying that voice could not be understood.

## Root cause

Mitra 0.19.0 selected `createOnDeviceSpeechRecognizer()` whenever Android reported that any on-device recognizer was available and always sent `EXTRA_PREFER_OFFLINE=true`. Android does not guarantee that the requested Gujarati or English model is installed. A missing language model therefore produced repeated recognition errors, and hands-free mode restarted the recognizer continuously.

## Changes

- Prefer Android's normal system speech recognizer so it can use either an installed offline language pack or the network speech service.
- Force offline mode only on a device where no normal recognizer exists and an on-device-only recognizer is the sole option.
- Do not cancel an idle recognizer before every start.
- Ignore the expected `ERROR_CLIENT` callback caused by an intentional cancel.
- Handle Android speech errors 10–15 with specific Gujarati guidance.
- Include the Android error code for unknown failures.
- Stop hands-free mode after language, timeout, no-match, network, or unknown errors instead of restarting forever.
- Allow at most one delayed automatic retry for temporary BUSY/CLIENT errors.
- Recreate the recognizer after service, audio, language, or connection failures.

No database migration is required. Existing books, prepared chapters, practice progress, and settings remain unchanged.
