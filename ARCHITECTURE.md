# Mitra architecture

Mitra is a personal, single-child, local-first Android learning companion for Standard 2 Gujarati-medium books.

## Product principle

`curiosity → child thinks → child tries → targeted hint → discovery → real-world/book activity → reflection`

The application should help the child put the phone down rather than maximize screen time.

## Runtime boundaries

- Native Android/Kotlin/Jetpack Compose.
- Room stores structured books, curriculum, mastery, sessions, attempts, and review state.
- DataStore stores non-secret preferences.
- App-private files store imported PDFs, thumbnails, and offline question banks.
- No Firebase, Supabase, PostgreSQL, login account, browser, feed, advertising, or social feature.
- `LearningEngine` owns concept selection, answer evaluation, mastery, retries, and review scheduling.
- `AiGateway` may analyze prepared textbook material and generate grounded activities, but never assigns mastery.
- Built-in Standard 2 activities remain local/deterministic.
- `SpeechInput` and `SpeechOutput` isolate Android speech services.

## Book lifecycle

1. Parent selects a PDF with Storage Access Framework.
2. App copies it to `files/books/{bookId}/source.pdf` and calculates SHA-256.
3. Parent selects contents pages or creates chapters manually.
4. AI chapter suggestions remain editable before persistence.
5. Parent prepares one chapter at a time.
6. Pages are rendered in bounded batches and sent to `AiGateway.analyzeChapter`.
7. Structured page knowledge and concepts are stored in Room.
8. Parent can enable/disable extracted concepts.
9. `BookPreparationService` generates a reusable offline question bank and stores it under `files/question_bank`.
10. Re-preparing a chapter invalidates stale page knowledge/concepts/question-bank files.

## Learning lifecycle

1. Child starts book-priority learning, local skill practice, or a parent-selected concept.
2. `ConceptSelector` checks prerequisites, due spaced reviews, mastery, and recent practice.
3. `QuestionVarietyPolicy` removes exact/recent duplicates.
4. Built-in questions are generated locally; prepared-book activities use cached questions first and remote generation only when needed.
5. `ActivityPlanPolicy` sanitizes physical/drawing missions and preserves off-screen variety.
6. Child answers with text, choice, confirmed speech, physical participation, book exploration, or rough work.
7. Numeric/choice/text/keyword activities are assessed locally. Participation-only activities never change mastery.
8. Incorrect numeric work may be classified as carry, borrow, place-value, reversed-digit, or table error and receives one targeted retry.
9. `SpacedReviewPolicy` updates the next review date.
10. Structured attempts/session summaries are persisted; raw audio and rough-work strokes are not.

## Guided maths

`LearningQuestion.arithmeticWork` activates two temporary workspaces:

- a freehand grid notebook
- guided fields for ones, carry/borrow, and tens

`GuidedMathCoach` checks each column independently. The final answer remains separate from the scratch work.

## Study Talk

`StudyLocalResponder` runs before remote retrieval and handles safe deterministic Standard 2 maths and mobile-game guidance locally.

Other questions flow through:

`child utterance → StudyContextService local retrieval → bounded prepared-page excerpts → AiGateway → grounded response`

Hands-free mode is turn-based: Android recognition ends after one utterance, TTS speaks, then listening starts again. The conversation is limited by the same learning-time policy as normal sessions. Recent turns are held in memory only.

## Room schema

- **v1:** books
- **v2:** concepts, prerequisites, mastery, sessions, attempts
- **v3:** chapters, page knowledge, `concepts.practiceReady`
- **v4:** `attempts.questionFingerprint` for repetition control
- **v5:** `mastery.nextReviewAt`, `reviewIntervalDays`, `consecutiveSuccesses`

Explicit migrations:

`1 → 2`, `2 → 3`, `3 → 4`, `4 → 5`

Release behavior must never use destructive migration.

## Backup boundary

The parent-triggered ZIP backup contains:

- Room database
- books directory
- offline question bank
- non-secret learning settings and voice style

It excludes API credentials, parent PIN, app signing key, raw audio, and in-memory conversation state.

## Safety constraints

- No child web search or arbitrary URL opening.
- No location, contacts, school/address collection, or raw voice retention.
- Child can always stop immediately.
- Physical missions are locally allowlisted/sanitized.
- No streak pressure, loot boxes, infinite feeds, variable rewards, or push-notification pressure.
- Mobile-game questions should redirect gently to the current learning activity and balanced offline play, not shame the child.

# Milestone 16 Addendum — Offline Local AI

`AiGateway` now has three parent-selectable production paths: OpenAI, Cloudflare Workers AI, and Offline Local. Offline Local never receives arbitrary internet access. It retrieves only prepared `PageKnowledgeEntity` text and either returns a deterministic extractive answer or asks a parent-imported LiteRT-LM model to phrase a short grounded Gujarati response.

The local model is copied to `files/local_ai/mitra-local.litertlm`; it is deliberately excluded from backups and APK packaging. `LiteRtLocalModel` serializes access with a mutex, keeps one engine warm, reloads when the imported file changes, tries GPU first, and falls back to CPU. New PDF image analysis remains a cloud/manual workflow until a supported local multimodal preparation pipeline is implemented.

Study Talk provider errors are child-safe: cloud parsing/network failures fall back to `OfflineStudyAnswerer`, and technical error text is not displayed when prepared local sources can answer.

# Milestone 17 Addendum — Provider capability guards

PDF preparation is now capability-gated before any page bitmap is rendered or any chapter status is changed. Offline Local advertises Study Talk and practice generation, but not contents-page or chapter image analysis. The parent UI observes the selected provider, disables unsupported actions, and keeps manual chapter entry available.

If a provider rejects preparation, the chapter remains unchanged. If re-preparing an existing `READY` chapter fails later, Mitra restores `READY` so cached page knowledge, concepts, and offline questions remain usable. New chapters still move to `FAILED` after a genuine preparation failure.
