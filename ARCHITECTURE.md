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
5. Parent prepares one chapter or all saved chapters sequentially.
6. Remote vision providers receive bounded JPEG batches; Offline Local receives embedded PDF text or Gujarati/English OCR text.
7. Structured page knowledge and concepts are stored in Room.
8. Parent can enable/disable extracted concepts.
9. `BookPreparationService` generates a reusable offline question bank and stores it under `files/question_bank`.
10. Re-preparing a chapter replaces stale page knowledge/concepts/question-bank files only after successful analysis; failed re-preparation preserves an existing READY state.

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

The local model is copied to `files/local_ai/mitra-local.litertlm`; it is deliberately excluded from backups and APK packaging. `LiteRtLocalModel` serializes access with a mutex, keeps one engine warm, reloads when the imported file changes, tries GPU first, and falls back to CPU. At Milestone 16, new PDF analysis remained a cloud/manual workflow. Milestone 18 supersedes that limitation with local PDF text extraction and OCR; the local model itself remains text-only rather than multimodal.

Study Talk provider errors are child-safe: cloud parsing/network failures fall back to `OfflineStudyAnswerer`, and technical error text is not displayed when prepared local sources can answer.

# Milestone 17 Addendum — Provider capability guards

At Milestone 17, PDF preparation became capability-gated before any page bitmap was rendered or any chapter status changed. Offline Local advertised Study Talk and practice generation but not contents-page or chapter image analysis. Milestone 18 retains those image guards while adding separate on-device text-analysis capabilities.

If a provider rejects preparation, the chapter remains unchanged. If re-preparing an existing `READY` chapter fails later, Mitra restores `READY` so cached page knowledge, concepts, and offline questions remain usable. New chapters still move to `FAILED` after a genuine preparation failure.

# Milestone 18 Addendum — On-device PDF text and OCR preparation

Offline Local now advertises text-based contents analysis and chapter analysis. It still does not accept page images directly. `BookPreparationService` selects the provider path before processing pages:

- image-capable remote/mock provider → bounded JPEG rendering → existing vision gateway
- Offline Local → embedded PDF text extraction → OCR only when text is unusable → text gateway

`AndroidOfflinePageTextExtractor` loads the app-private PDF with PDFBox and reads only the requested pages. Each page passes a minimum useful-text check. A page that fails that check is rendered at a bounded width and passed to `TesseractOcrEngine`. Tesseract is serialized behind a mutex and kept warm; Gujarati and English language data are copied from APK assets to an app-private versioned directory on first use.

Offline chapter preparation then follows:

`PDF page → embedded text or Gujarati/English OCR → 4-page text batch → OfflineAiGateway → Room page knowledge/concepts → offline question bank`

When a compatible `.litertlm` file exists, `OfflineAiGateway` asks it for strict JSON and validates every returned page number and concept range. Missing or malformed model output falls back page-by-page to deterministic extraction. Without a model, the deterministic path still stores readable page text, short summaries, exercises, and conservative concepts. This fallback does not claim semantic understanding beyond the extracted text.

The parent can prepare one chapter or all saved chapters. Full-book preparation remains sequential to bound memory and native-model pressure. Existing `READY` chapters retain their ready state and cached data after a failed re-prepare.

The Room schema remains version 5 because the new extraction method is transient request metadata; persisted page-knowledge and concept entities are unchanged.

# Milestone 19 Addendum — Two-way voice tutor

The child-facing Study Talk route now combines free-form grounded Q&A with an explicit challenge state machine:

`topic selection or spoken intent → MitraVoicePracticeService → MitraVoiceChallenge → TTS → speech recognition → MitraPracticeEvaluator → adaptive feedback → next challenge`

`MitraVoicePracticeService` owns challenge creation but no child profile data. Built-in table, number-neighbor, and spelling challenges are deterministic. Prepared-book challenges come from the local `OfflineQuestionBank`; when its cache is empty, the service passes bounded prepared page text to `AiGateway.createPracticeQuestions` and stores the generated questions for reuse.

`MitraPracticeEvaluator` performs numeric, exact-text, spelling, and keyword evaluation locally. It allows one hinted retry. The second wrong attempt reveals the correction method and advances, preventing an endless failure loop. Praise varies deterministically and may mention a short in-session streak; the streak is not persisted and does not unlock rewards.

Speech input is language-aware per challenge. Gujarati uses `gu-IN`, while English spelling recognition uses `en-IN`. Mitra prefers Android's normal system recognizer so the device can use either an installed offline pack or a network recognition service. An on-device-only recognizer is used only when the normal recognizer is unavailable. Raw audio remains outside Room, backups, and analytics.

## Mitra 0.19.1 voice recognition reliability

`AndroidSpeechInput` now prefers `SpeechRecognizer.createSpeechRecognizer()` so Android can choose an installed offline pack or a remote recognition service. It uses `createOnDeviceSpeechRecognizer()` only when no normal recognizer is available. The Study Talk view model allows at most one automatic retry for transient BUSY/CLIENT failures; all other errors stop hands-free mode and wait for a deliberate microphone tap. This prevents recognizer request storms and preserves typed input as a fallback.

# Milestone 20 Addendum — Smart tutor, tests and visual teaching

Study Talk now has one shared state machine for child-led grounded questions and Mitra-led challenges. An active challenge is temporarily retained when the child asks an explanatory/textbook question, then restored after the answer. Challenge length is explicit (20 or 25), and all evaluation remains local through `MitraPracticeEvaluator`.

Parent-created tests are persisted as one app-private, Base64-safe text plan outside Room so no schema migration is needed. `ParentQuizService` builds questions either from the same grounded/offline practice service used by Study Talk or from a parent-selected built-in Standard 2 skill through `Standard2SkillActivityFactory`. `ChildQuizViewModel` awards one mark per question, allows one retry, speaks correction/praise and advances automatically.

The general learning engine defaults to 20 questions and accepts up to 25. Arithmetic rough work keeps ephemeral drawing strokes only in Compose state. Place-value answer and carry/borrow inputs are rendered in the actual column layout rather than in a detached result field.

`ShadowLessonScreen` is a deterministic Canvas/TTS lesson, not generated child content. Its six steps are bounded and repeatable. Colour and sentence activities similarly use local allow-listed learning data and do not persist free-form speech.

Child home requests lock-task mode. On ordinary consumer devices this is screen pinning; policy-enforced lock task requires device-owner provisioning. Parent PIN or Android device-credential confirmation exits the app-level gate and releases lock task.

Room remains schema 5. The only new persisted file is the parent quiz plan under app-private storage.
