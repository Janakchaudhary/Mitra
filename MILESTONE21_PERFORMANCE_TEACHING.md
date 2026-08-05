# Mitra 0.21.0 — Performance and Teaching Upgrade

Implemented in this milestone:

- Persistent per-page embedded-text/OCR cache keyed by PDF identity and page number.
  Re-preparing a chapter no longer repeats OCR for unchanged pages.
- Bounded prepared-book candidate retrieval. Study Q&A no longer loads and scores every
  prepared page from every book; it queries likely pages first and applies Gujarati/OCR
  tolerant ranking only to a capped candidate set.
- In-memory LRU cache for prepared question-bank JSON files, avoiding repeated disk parsing
  during 20/25-question tests and voice practice.
- Child-friendly English sentence evaluation that accepts natural article/word-order
  variation when the required subject/action/object words are present, with specific missing
  word feedback.
- Release R8 minification and resource shrinking enabled.
- Version updated to 0.21.0 (versionCode 41).

Data compatibility:

- Room schema remains version 5.
- Existing books, chapters, progress, mastery and question-bank files are preserved.
- OCR cache is disposable and automatically invalidates when the source PDF changes.

Next architecture milestone (not falsely claimed as complete here):

- `.mitrabook` import/export UI and package validation.
- Room-backed vocabulary/question tables and FTS virtual table.
- WorkManager-based resumable full-book preparation.
- Baseline Profile and Macrobenchmark modules.
