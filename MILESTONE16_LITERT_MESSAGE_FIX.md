# Milestone 16 LiteRT-LM Message API CI Fix

## Problem

`litertlm-android:0.14.0` returns a `Message` from `Conversation.sendMessage(...)`, but the compiled Android artifact does not expose a Kotlin `text` property. This caused:

```text
Unresolved reference 'text'
```

## Fix

Use the response object's documented printable representation:

```kotlin
conversation.sendMessage(prompt)
    .toString()
    .trim()
```

The LiteRT-LM Kotlin examples print synchronous messages directly and use `Message.toString()` for streamed messages.

## Version

- versionCode: 31
- versionName: 0.16.4
- Room schema: unchanged
