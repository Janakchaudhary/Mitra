# Mitra architecture

Mitra is a personal, single-child, local-first Android learning companion for Standard 2 Gujarati-medium books.

## Product principle

`curiosity -> child thinks -> child tries -> hint -> discovery -> real-world activity -> book -> reflection`

The goal is not to maximize screen time. Learning sessions should increasingly direct the child to the physical book, pencil, toys and real objects.

## Runtime boundaries

- Android APK owns books, curriculum, progress, settings and lesson state.
- Room stores structured data.
- DataStore stores simple preferences.
- App-private files store imported PDFs and thumbnails.
- No external database/account/cloud synchronization.
- `LearningEngine` owns concept selection, evaluation and mastery updates.
- `AiGateway` is replaceable and must never own curriculum/mastery decisions.
- `SpeechInput` and `SpeechOutput` isolate Android speech services from session logic.
- Milestone 4 still uses `MockAiGateway`; remote textbook understanding is not enabled yet.

## Book lifecycle — Milestone 4

1. Parent unlocks Parent Mode.
2. Parent chooses a PDF using Android Storage Access Framework.
3. App copies PDF into `files/books/{uuid}/source.pdf`.
4. App computes SHA-256; duplicate hashes are rejected.
5. App reads page count with `PdfRenderer` and renders a cover thumbnail.
6. Book metadata is stored in Room.
7. Parent opens **Set up chapters**.
8. Parent previews PDF pages and marks one or more contents/index pages.
9. `BookPreparationService` renders only those pages and calls `AiGateway.analyzeTableOfContents`.
10. Suggestions become editable `ChapterDraft` objects; parent can also add/remove chapters manually.
11. Saved chapters are persisted locally. Changed chapter ranges invalidate stale page knowledge/concepts.
12. Parent prepares one chapter at a time. Pages are rendered in chunks of four and sent through `AiGateway.analyzeChapter`.
13. Structured page knowledge and concepts are cached in Room.
14. Book/chapter preparation status is updated locally.

`MockAiGateway` clearly labels its results and creates book concepts with `practiceReady = false`. This lets the full pipeline be tested without pretending the mock provider understood a child's textbook. Milestone 5 can replace the provider and enable validated book concepts without changing the database/session architecture.

## Learning lifecycle

1. Child presses **રમીએ**.
2. Local built-in curriculum is seeded if needed.
3. Only `practiceReady` concepts are eligible.
4. `ConceptSelector` considers prerequisites and mastery.
5. `LearningEngine` creates a persisted session.
6. The current gateway creates practice questions.
7. Child can type or speak a Gujarati answer.
8. Numeric answers are evaluated locally.
9. Attempts and mastery are stored in Room.
10. Session is completed or stopped and persisted.

## Room schema

Version 1:
- books

Version 2:
- books
- concepts
- concept_prerequisites
- mastery
- learning_sessions
- attempts

Version 3:
- all v2 tables
- chapters
- page_knowledge
- `concepts.practiceReady`

Explicit `1 -> 2` and `2 -> 3` migrations preserve installed app data.

## Milestones

### Milestone 1 — complete
Parent PIN, child/parent navigation, private PDF import, duplicate detection, Room book library, PDF viewer and delete.

### Milestone 2 — complete
Local curriculum, prerequisites, mastery, sessions, attempts, learning engine, deterministic evaluation and mock tutor practice UI.

### Milestone 3 — complete
Push-to-talk Gujarati recognition, Gujarati TTS, spoken stop commands and text fallback.

### Milestone 4 — complete in this source
Parent-controlled contents-page selection, editable/manual chapters, chapter preparation, page-knowledge cache, book-derived concept storage, schema v3 and mock analysis boundary.

### Milestone 5 — next
Real parent-configured AI provider for textbook image understanding and grounded Gujarati tutoring. Structured output must be validated, and only valid analyzed concepts may be enabled for child practice.

## Safety constraints

- No browser/web-search tools in child mode.
- No location, contacts, school/address collection or raw voice retention.
- Child can always stop immediately.
- Physical missions must come from a safe allowlist.
- No streaks, loot boxes, infinite feeds, variable rewards or pressure mechanics.
