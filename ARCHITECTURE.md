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
- Milestone 2 uses `MockAiGateway`, so practice is fully local/offline.

## Book lifecycle

1. Parent unlocks Parent Mode.
2. Parent chooses a PDF using Android Storage Access Framework.
3. App copies PDF into `files/books/{uuid}/source.pdf`.
4. App computes SHA-256; duplicate hashes are rejected.
5. App reads page count with `PdfRenderer` and renders a cover thumbnail.
6. Book metadata is stored in Room.
7. Future milestones add TOC/chapter analysis incrementally, never mandatory full-book upload.

## Learning lifecycle — Milestone 2

1. Child presses **રમીએ**.
2. Local built-in curriculum is seeded if needed.
3. `ConceptSelector` considers prerequisites and mastery.
4. `LearningEngine` creates a persisted session.
5. `MockAiGateway` creates five deterministic Gujarati practice questions.
6. Child types an answer using Gujarati or English digits (common Gujarati number words are also accepted).
7. App evaluates the answer locally.
8. Attempt is stored in Room.
9. `MasteryPolicy` updates mastery; the gateway never writes mastery.
10. Session is completed or stopped and persisted.

The built-in curriculum is temporary scaffolding. Book-derived concepts added later use the same `ConceptEntity`/mastery/session architecture.

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

A non-destructive `1 -> 2` migration preserves existing book data.

## Milestones

### Milestone 1 — complete
Parent PIN, child/parent navigation, private PDF import, duplicate detection, Room book library, PDF viewer and delete.

### Milestone 2 — complete in this source
Local curriculum, prerequisites, mastery, sessions, attempts, learning engine, deterministic evaluation and mock tutor practice UI.

### Milestone 3 — next
Push-to-talk Gujarati speech input/output behind interfaces. Keep text input as fallback. Do not add remote AI yet.

### Milestone 4
Parent-controlled chapter setup and incremental book analysis through a replaceable AI gateway.

### Milestone 5+
Grounded tutor AI, safe activities, physical missions, attention/session limits and parent progress dashboard.

## Safety constraints

- No browser/web-search tools in child mode.
- No location, contacts, school/address collection or raw voice retention.
- Child can always stop immediately.
- Physical missions will come from a safe allowlist.
- No streaks, loot boxes, infinite feeds, variable rewards or pressure mechanics.
