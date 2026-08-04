# Milestone 20 — Smart Tutor and Parent Test Builder

Version **0.20.0** (`versionCode 38`)

Milestone 20 turns Mitra into a more complete, local-first teaching flow for one Standard 2 child while preserving the existing Room schema and prepared-book data.

## Child tutor

- “મિત્રને પૂછીએ” supports child-led questions and Mitra-led practice in the same conversation.
- Prepared-book answers are retrieved only from relevant `READY` page knowledge; unrelated first pages are not passed to the answerer.
- Tables, before/after numbers and spelling are evaluated locally.
- Voice practice supports 20 or 25 questions, one guided retry, a clear correction, varied praise and automatic movement to the next question.
- A child can interrupt a Mitra-led quiz with a textbook question; Mitra answers and then resumes the current challenge.

## Learning games

- **રંગોની મજા:** identify, say and spell colours using voice or typing.
- **English Sentence Builder:** picture/emoji prompt, spoken or typed complete sentence, grammar help and word chips.
- **કૌશલ્ય રમત arithmetic:** carry/borrow appears above the tens column and result boxes sit in the actual ones/tens answer row. Arithmetic questions avoid page scrolling on normal phone heights; the optional freehand pad stays collapsed.
- Correct voice answers receive spoken feedback and move forward automatically.
- Standard sessions and mixed skill sessions target 20 questions; generators accept up to 25.

## Parent tools

- The final known PIN digit triggers verification automatically.
- Parent area can also use Android's device credential confirmation when the phone is secured.
- Parent Test Builder creates a 20- or 25-mark test from any selected built-in Standard 2 skill, prepared books, tables, number neighbours, spelling or mixed practice.
- Each child test question is one mark and accepts voice or typing.

## Visual lesson

- New animated and narrated **પડછાયો** lesson explains light source, object, shadow direction, shadow length and a safe observation activity in six steps.

## Book preparation

- Existing printed-page to physical-PDF mapping is retained.
- Generic stale chapter ranges can be repaired by scanning early contents pages locally before preparation.
- Gujarati combining marks are preserved during retrieval and simple stem/edit-distance scoring improves OCR-word matching.
- Preparation stores up to 20 reusable questions per practice-ready concept.

## Child-mode containment

Mitra requests Android lock-task/screen-pinning while the child home is active and exits it after parent authentication. On a normal personal phone this is screen pinning; fully enforced kiosk mode requires device-owner provisioning by Android.

## Data and privacy

- Room schema remains **5**; no migration is required.
- Existing books, prepared chapters, progress and settings are preserved.
- Raw microphone audio, freehand rough-work strokes and temporary conversation turns are not persisted.
