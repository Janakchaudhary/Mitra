# Milestone 14 — App Logo and Native Splash Screen

Version: `0.14.0` (`versionCode 23`)

## Added

- New Mitra launcher icon featuring a friendly reading mascot.
- Adaptive launcher icon for Android 8+.
- Legacy launcher icon fallback.
- Native Android splash screen using `androidx.core:core-splashscreen`.
- Splash remains visible only while local setup/PIN state is checked.
- Short fade-and-scale handoff into the Compose UI.
- No artificial loading delay.

## Upgrade

No Room database migration is required. Install the GitHub-built, persistently signed APK over the existing stable-signed Mitra installation.
