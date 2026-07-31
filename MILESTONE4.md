# Milestone 4 — Book preparation pipeline

Milestone 4 turns an imported PDF into local chapter/page/concept data without changing the child learning engine.

## Added

- Room schema v3 with `chapters` and `page_knowledge` tables.
- Non-destructive v2 → v3 migration; existing books, PIN, mastery, attempts and sessions remain intact.
- `practiceReady` flag on concepts. Built-in concepts remain playable; book-derived concepts can be prepared now and enabled by the real tutor provider later.
- Parent book setup screen with PDF preview.
- Mark one or more table-of-contents/index pages.
- Chapter detection behind `AiGateway`.
- Editable chapter titles and start/end page ranges.
- Manual chapter creation/removal.
- Chapter preparation in page chunks.
- Local caching of page summaries and book-derived concepts.
- Book/chapter preparation statuses.
- Deleting a book also removes chapter/page/concept knowledge.
- Tests for chapter range resolution and practice-ready concept filtering.

## Important Milestone 4 limitation

`MockAiGateway` is still the active provider. It deliberately does **not** pretend to understand the textbook images. It exercises the full pipeline with clearly-labelled mock chapter suggestions and mock page/concept records. Those generated book concepts have `practiceReady = false`, so the child will not receive fake questions about the real textbook.

Milestone 5 should connect a real parent-configured AI provider for TOC/chapter understanding and tutoring. Once a chapter is analyzed by a real provider, its concepts can be marked `practiceReady = true` and used by the existing learning engine.

## Physical test

1. Install Milestone 4 over Milestone 3.
2. Parent → My books → open an imported PDF.
3. Tap **Set up chapters**.
4. Browse to the contents/index page and mark it.
5. Tap **Detect chapter structure**.
6. Review/edit chapter titles and ranges, or add chapters manually.
7. Save.
8. Back on Book Detail, tap **Prepare** for one chapter.
9. Confirm the chapter becomes READY and preparation remains after app restart.
10. Confirm existing voice learning still works and uses the built-in curriculum until Milestone 5 enables real book-derived concepts.


## CI fix in 0.4.1

- Removed explicit `androidx.compose.foundation.layout.weight` imports; `.weight(...)` remains used only inside `RowScope` / `ColumnScope`.
- Added direct BOM-managed `androidx.compose.foundation:foundation-layout` dependency.
- Made `AndroidSpeechInput.stopListening()` explicitly return `Unit`, avoiding `Unit?` inference from the safe call.
- Added `SpeechInputContractTest`.
