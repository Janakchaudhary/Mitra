# Milestone 14 lint fix (v0.14.2)

Fixed `UnusedContentLambdaTargetStateParameter` in `SentenceBuilderScreen.kt`.

The `AnimatedContent` content lambda now names and uses `targetIndex` to select the puzzle, calculate remaining words, success state, and shuffle seed for the corresponding animated state. This makes old/new content genuinely state-specific during the transition.

No Room migration is required.
