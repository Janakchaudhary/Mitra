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
- `AiGateway` can use mock/offline behavior or the parent-configured remote provider. Child answers are never sent through the gateway.

## Book lifecycle

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

`MockAiGateway` clearly labels its results and keeps mock book concepts disabled. A configured remote provider can enable validated concepts. Chapters can be prepared again when activity capabilities change.

## Learning lifecycle

1. Child presses **રમીએ** for book-priority learning or **કૌશલ્ય રમત** for the local Standard 2 skill engine.
2. Local built-in curriculum is seeded if needed.
3. Only `practiceReady` concepts are eligible.
4. `ConceptSelector` considers prerequisites and mastery.
5. `LearningEngine` creates a persisted session.
6. The current gateway creates a bounded activity plan grounded in local prepared book knowledge.
7. `ActivityPlanPolicy` sanitizes physical/drawing activities and ensures off-screen variety.
8. Child can type, choose, speak, explore, draw, use the book, or teach Mitra depending on the activity.
9. Numeric/choice/short-text/keyword answers are evaluated locally; participation activities are recorded as `UNKNOWN`.
10. Only assessed results affect mastery. Attempts and summaries are stored in Room.
11. Session is completed or stopped and persisted.

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

### Milestone 4 — complete
Parent-controlled contents-page selection, editable/manual chapters, chapter preparation, page-knowledge cache, book-derived concept storage, schema v3 and mock analysis boundary.

### Milestone 5 — complete
Parent-configured remote textbook image analysis and book-grounded activity generation with structured output and local privacy boundaries.

### Milestone 6 — complete
Mixed activity types, local non-numeric evaluation, hints, participation-only mastery protection, Teach-Mitra, drawing, book exploration, and local physical-mission safety policy.

### Milestone 7 — complete
Parent-only local progress analytics: time spent, accuracy, subject/concept mastery, weak/strong concepts, recent sessions and prerequisite-aware next-practice recommendation. No remote AI call is required to calculate progress.

## Safety constraints

- No browser/web-search tools in child mode.
- No location, contacts, school/address collection or raw voice retention.
- Child can always stop immediately.
- Physical missions must come from a safe allowlist.
- No streaks, loot boxes, infinite feeds, variable rewards or pressure mechanics.

## Milestone 8 implementation note

Runtime learning limits are stored in local DataStore. `LearningLimitPolicy` independently enforces daily allowance and per-session maximums. Parent access uses an in-memory `ParentAccessManager`: successful PIN verification unlocks parent routes temporarily, and the access state is cleared after a short background grace period, while brief system surfaces such as the PDF picker can return without losing parent context. Parent settings also expose local reset operations. None of these dashboard/limit/reset features require an AI request.


### Milestone 9 — complete in this source
The offline Standard 2 skill engine expands built-in curriculum into separate mastery concepts for two-digit addition/subtraction (including carrying/borrowing), missing numbers, comparison, word problems, multiplication meaning, tables 2–10, Gujarati spelling/reading/language skills, and English spelling/reading skills. Built-in skill sessions never require the remote AI provider. Dictation activities separate visible prompt text from TTS text so spelling answers are not exposed on screen. Parent progress lists each built-in skill independently.
