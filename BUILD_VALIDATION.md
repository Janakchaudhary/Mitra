# Mitra 0.20.0 Validation

This source package was prepared as Milestone 20 (`versionCode 38`, `versionName 0.20.0`).

## Checks completed

- Scanned all 172 Kotlin source/test files for unresolved merge markers and malformed edit remnants.
- Verified the local answer evaluator for Gujarati number words, multiplication-table answers and spoken English spelling.
- Verified prepared-book retrieval keeps the relevant lesson page and excludes unrelated front-matter text for a Gujarati word-meaning query.
- Verified the offline glossary answer for `દંગોરો`.
- Verified Standard 2 generators can create 20 regrouping-addition questions and 20 multiplication-table questions.
- Verified Parent Test Builder can create exact 20- and 25-mark tests from selected built-in skills.
- Verified the colour activity contains the requested answer among its visible choices.
- Verified Sentence Builder contains 20 prompts and the animated shadow lesson contains six teaching steps.
- Verified the packaged source archive and SHA-256 checksum after creation.

## Android build note

A full `assembleDebug`/Lint run was not executed in this packaging environment because the supplied project has no Gradle wrapper and the environment has no Android SDK or network access for installing them.

The included GitHub Actions workflow installs Java, Gradle and Android SDK components and runs the Android build. The equivalent local commands are:

```bash
gradle testDebugUnitTest lintDebug assembleDebug
```

No Room schema migration is required; the database remains at schema version 5.
