# Milestone 17 — Safe Offline Book Preparation

## Implemented

- Added explicit AI capabilities for contents-page image analysis, chapter image analysis, practice generation, and Study Talk.
- Offline Local declares PDF image analysis unsupported while retaining offline practice and grounded Study Talk.
- Chapter detection and preparation now check capability before rendering PDF pages.
- Offline Local no longer changes a chapter to `PREPARING` or `FAILED` when preparation is unsupported.
- A failed re-prepare preserves an existing `READY` chapter and its cached knowledge.
- Parent book screens react to provider changes, disable unsupported actions, and explain the manual/online alternatives.
- Manual chapter title and page-range entry remains available offline.
- Added unit coverage for capability policy and persistence/rendering guards.

## Release

- `versionCode = 32`
- `versionName = 0.17.0`
