# Milestone 16 Kotlin / LiteRT-LM compatibility fix

## Failure

`litertlm-android:0.14.0` is compiled with Kotlin metadata 2.3.0, while the
project previously compiled with Kotlin 2.1.20. Kotlin therefore rejected the
LiteRT-LM `Engine`, `Conversation`, and `Message` classes before compiling the
app.

## Fix

- Kotlin Android plugin: `2.3.21`
- Kotlin Compose compiler plugin: `2.3.21`
- Kotlin kapt plugin: `2.3.21`
- Android Gradle Plugin: `8.13.2`
- Gradle used in GitHub Actions/Codespaces: `8.13`
- LiteRT-LM remains pinned to `0.14.0`

AGP 8.13.2 is used because it contains the R8 support required for Kotlin 2.3.
The project does not use `-Xskip-metadata-version-check`; suppressing the check
would hide an actual binary compatibility mismatch.

App version: `0.16.1` (`versionCode 28`). No Room migration is required.
