# GitHub Actions Node 24 CI fix

The Android build workflow was updated after GitHub began forcing JavaScript actions that still target Node.js 20 to run on Node.js 24.

## Changes

- `actions/checkout@v4` -> `actions/checkout@v6`
- `actions/setup-java@v4` -> `actions/setup-java@v5`
- removed `android-actions/setup-android@v3`
- `gradle/actions/setup-gradle@v4` -> `gradle/actions/setup-gradle@v6`
- `actions/upload-artifact@v4` -> `actions/upload-artifact@v7`
- runner pinned to `ubuntu-24.04`
- Android SDK 35 / Build Tools 35.0.0 are verified and installed with `sdkmanager` only if missing
- workflow permissions reduced to `contents: read`

GitHub-hosted Ubuntu 24.04 runners already provide `ANDROID_HOME`, Android command-line tools, Android platform 35 and Build Tools 35.0.0, so a separate Android setup JavaScript action is unnecessary for this project.
