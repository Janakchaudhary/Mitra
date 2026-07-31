# Milestone 4 CI Fix — 0.4.1

Fixes GitHub Actions compilation failures reported from Milestone 3 and carried into Milestone 4.

## Compose `weight` compile error

Removed explicit imports of:

```kotlin
import androidx.compose.foundation.layout.weight
```

`Modifier.weight(...)` is a scoped modifier supplied by `RowScope` / `ColumnScope`; the calls remain inside those scopes and do not need the problematic explicit import. The project also declares `androidx.compose.foundation:foundation-layout` directly, with its version managed by the existing Compose BOM.

Affected files:

- `LearningSessionScreen.kt`
- `BookDetailScreen.kt`
- `BookSetupScreen.kt`

## `SpeechInput.stopListening()` override

Changed `AndroidSpeechInput.stopListening()` to explicitly return non-null `Unit`:

```kotlin
override suspend fun stopListening(): Unit = withContext(Dispatchers.Main.immediate) {
    recognizer?.stopListening()
    Unit
}
```

The prior expression body could infer `Unit?` because `recognizer?.stopListening()` is a safe call, which does not satisfy the interface's `suspend fun stopListening(): Unit` contract.

## Validation commands

```bash
gradle clean testDebugUnitTest lintDebug assembleDebug
```
