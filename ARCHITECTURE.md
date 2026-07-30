# Mitra architecture

Mitra is a personal, single-child, local-first Android learning companion for Standard 2 Gujarati-medium books.

## Product principle

`curiosity -> child thinks -> child tries -> hint -> discovery -> real-world activity -> book -> reflection`

The goal is not to maximize screen time. Later learning sessions should deliberately direct the child to the physical book, pencil, toys and real objects.

## Runtime boundaries

- Android APK owns books, progress, settings and lesson state.
- Room stores structured data.
- DataStore stores simple preferences.
- App-private files store imported PDFs and thumbnails.
- No external database/account/cloud synchronization.
- A future `AiGateway` will be replaceable; it must not own curriculum/mastery decisions.

## Book lifecycle

1. Parent unlocks Parent Mode.
2. Parent chooses a PDF using Android Storage Access Framework.
3. App copies PDF into `files/books/{uuid}/source.pdf`.
4. App computes SHA-256; duplicate hashes are rejected.
5. App reads page count with `PdfRenderer` and renders a cover thumbnail.
6. Book metadata is stored in Room.
7. Future milestones add TOC/chapter analysis incrementally, never mandatory full-book upload.

## Milestones

### Milestone 1 — current
- First-run parent PIN.
- Child home / parent mode.
- Add multiple PDF books.
- Copy PDFs into private storage.
- Duplicate SHA-256 detection.
- Local Room library.
- PDF page viewer.
- Remove book and private files.
- GitHub Actions debug APK build.

### Milestone 2
Add local curriculum, concepts, mastery, sessions, attempts, learning engine and a mock tutor.

### Milestone 3
Push-to-talk Gujarati speech input/output behind interfaces.

### Milestone 4
Parent-controlled chapter setup and incremental book analysis through a replaceable AI gateway.

### Milestone 5+
Tutor AI, grounded activities, safe physical missions, attention/session limits and parent progress dashboard.

## Safety constraints for future work
- No browser/web-search tools in child mode.
- No location, contacts, school/address collection or raw voice retention.
- Child can always stop immediately.
- Physical missions come from a safe allowlist.
- No streaks, loot boxes, infinite feeds, variable rewards or pressure mechanics.
