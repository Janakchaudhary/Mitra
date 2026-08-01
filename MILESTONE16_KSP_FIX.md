# Milestone 16 KSP / Room compiler fix

Version: `0.16.3` (`versionCode = 30`)

## Problem

After moving to Kotlin 2.3 for LiteRT-LM compatibility, the Room compiler still ran through kapt and `kaptDebugKotlin` failed without a useful source diagnostic.

## Fix

- Removed the Kotlin kapt Gradle plugin.
- Added KSP `2.3.9` (KSP2 is the default).
- Changed Room compiler configuration from `kapt(...)` to `ksp(...)`.
- Updated Room libraries to `2.8.4`.
- Kept the database schema version at 5; no migration is required.

This is a build-tooling change only. Existing books, progress, sessions, settings, and database migrations are unchanged.
