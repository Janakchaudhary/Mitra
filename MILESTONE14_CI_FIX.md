# Milestone 14 CI Fix — v0.14.1

## Failure

Kotlin compilation failed because `androidx.compose.foundation.layout.matchParentSize`
was imported as a top-level extension in four files. In the Compose version used by
this project, `matchParentSize` is a `BoxScope` member and is not available as that
import.

## Fix

Removed the invalid imports and replaced the four overlay modifiers with
`Modifier.fillMaxSize()`. Each overlay already lives inside a bounded `Box`, so this
preserves the intended full-overlay behavior without relying on the scope-specific
modifier.

Affected files:

- `ColorLabScreen.kt`
- `SentenceBuilderScreen.kt`
- `MitraMotion.kt`
- `LearningSessionScreen.kt`

Version bumped to `0.14.1` (`versionCode = 24`).
