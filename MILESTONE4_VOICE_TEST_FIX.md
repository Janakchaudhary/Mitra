# Milestone 4 CI voice-test fix (v0.4.2)

## Failure

`LearningSessionVoiceTest > session speaks first question and submits recognized answer`
failed after the application compiled successfully.

## Root cause

The fake tutor returns the feedback string:

```text
હા! સાચું.
```

The test used:

```kotlin
output.spoken.contains("સાચું")
```

`spoken` is a `List<String>`, so `List.contains()` checks whether a whole list element
is exactly equal to `"સાચું"`. It does not check whether any spoken message contains
that word. The actual element is `"હા! સાચું."`, therefore the assertion failed even
though voice feedback was correct.

## Fix

Use a predicate over the spoken messages:

```kotlin
assertTrue(output.spoken.any { it.contains("સાચું") })
```

This verifies the behavior the test intended: the child hears Gujarati feedback that
contains the word `સાચું`, while allowing friendly surrounding text.

No production voice/session code was changed for this failure.
