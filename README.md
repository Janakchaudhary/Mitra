# Mitra / મિત્ર

A local-first Android learning companion for one Standard 2 Gujarati-medium child.

## What is implemented

Milestone 1:
- First launch parent PIN.
- Child and PIN-protected parent areas.
- Add PDF books through Android document picker.
- Copy PDF into app-private storage.
- SHA-256 duplicate detection.
- Room book library.
- Local `PdfRenderer` viewer.
- Remove a book and its private files.
- GitHub Actions workflow that tests, lints and builds `app-debug.apk`.

AI and voice are intentionally not included yet.

## Build online with GitHub

1. Create a private GitHub repository.
2. Upload this project.
3. Push to `main`.
4. Open the **Actions** tab and run **Android build**.
5. Download the `mitra-debug-apk` artifact from the completed workflow.
6. Install `app-debug.apk` on your Android phone (you may need to allow installation from your browser/files app).

The GitHub workflow installs Java, Android SDK 35 and Gradle, so no local Android Studio is required for CI builds.

## Develop in Codespaces

The included `.devcontainer` installs Java 17, Gradle 8.11.1 and Android SDK 35. In a Codespace, Codex can run `gradle testDebugUnitTest lintDebug assembleDebug` directly. GitHub Actions performs the same checks on pushes.

## Local Android Studio

Open the repository in Android Studio and let Gradle sync. Use JDK 17+.

## Next task for Codex

Read `ARCHITECTURE.md` and `AGENTS.md`, then verify Milestone 1 by fixing any compilation/lint issues found by GitHub Actions. Do not begin Milestone 2 until `testDebugUnitTest`, `lintDebug`, and `assembleDebug` all pass.
