# Mitra Android

**Current milestone: 9 (v0.9.0)** — Standard 2 skill engine for two-digit arithmetic, carrying/borrowing, multiplication tables 2–10, Gujarati spelling/reading, and English spelling/reading.

મિત્ર

A local-first Android learning companion for one Standard 2 Gujarati-medium child.

## Current version — Milestone 9 (0.9.0)

Mitra now supports the full local-first path from PDF books to richer child learning sessions:

- Parent PIN and separate child/parent areas.
- Dedicated **કૌશલ્ય રમત** child mode that always uses the offline Standard 2 skill curriculum.
- Separate mastery concepts for two-digit addition/subtraction, carrying, borrowing, missing numbers, comparison and word problems.
- Multiplication meaning plus separate mastery for tables 2 through 10.
- Gujarati word recognition, dictated spelling, missing letters, read-aloud, sentence completion, meaning and singular/plural.
- English word recognition, dictated spelling, missing letters, read-aloud and sentence completion.
- Spelling prompts keep the answer hidden on-screen while TTS dictates the target word.
- Parent progress now lists every built-in Standard 2 skill, including not-started skills.
- Built-in skill drills remain local even when a remote AI provider is configured.
- Private PDF import, SHA-256 duplicate detection and local PDF viewer.
- Room curriculum, concepts, prerequisites, mastery, sessions and attempts.
- Gujarati push-to-talk and Gujarati TTS with text fallback; English skill activities can request `en-IN` speech/recognition.
- Spoken maths/table answers can be normalized locally from Gujarati number words through 100.
- Parent-reviewed chapter detection and manual chapter editing.
- Optional remote textbook page analysis through a replaceable `AiGateway` (OpenAI default, Cloudflare Workers AI secondary).
- Book-grounded activity generation from locally cached page knowledge.
- Numeric questions, multiple choice, short text, keyword checks, riddles, reading, vocabulary, stories, book-look, drawing, physical missions, Teach-Mitra and recap activities.
- Hints that reduce mastery gain on assessed answers.
- Participation-only activities recorded without changing mastery.
- Local safety replacement for physical/drawing instructions.
- Automatic off-screen activity injection for longer sessions.
- Parent-only local progress dashboard with subject/concept mastery, weak areas, recent sessions and next-practice recommendation.
- Configurable session/daily learning limits with local enforcement and countdown.
- Parent access relocks on timeout/background.
- Parent-only privacy/data reset controls.
- Offline child-home messaging and built-in local fallback when remote book question generation is unavailable.
- No Firebase, Supabase, PostgreSQL, account system, browser, feed, ads or social features.

## Important textbook preparation note

Milestone 5 marked reading/vocabulary/open-ended concepts `practiceReady=false` because only integer evaluation existed then. After upgrading from Milestone 5 or earlier, use **Prepare again** on a textbook chapter to let the new analysis rules enable appropriate language/story concepts.

## Build online with GitHub

1. Update your private GitHub repository with this project.
2. Push to `main`.
3. Open **Actions** and run **Android build**.
4. Download the `mitra-debug-apk` artifact.
5. Install `app-debug.apk` over the existing Mitra APK.

The workflow runs:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

## Recommended physical-device test

1. Upgrade over 0.8.0; do not uninstall first.
2. Confirm existing books/PIN/progress remain.
3. In Parent Mode, configure/test the AI provider if not already configured.
4. Open one real textbook chapter and press **Prepare again**.
5. Start **કૌશલ્ય રમત** and verify local tables/spelling, then start **રમીએ** for book-grounded practice.
6. Verify mixed activities appear, including a locally assessed question and an exploration/off-screen activity.
7. Use **સંકેત** on an assessed question.
8. Try a multiple-choice activity.
9. Complete a physical/drawing/Teach-Mitra activity and confirm it advances without claiming a correct answer.
10. Open **Parent → Learning progress** and verify the completed session appears with time, accuracy and mastery.
11. Say **બસ** in another session and confirm immediate exit still works.

## Privacy/runtime boundary

Books, prepared knowledge, progress and settings remain on the phone. Remote AI receives only the parent-selected book material/context required for analysis/activity generation. Child microphone recordings, child answer transcripts, mastery and session history are not sent to the remote model.

A parent-entered API key inside a mobile APK is only appropriate for this private development/personal-use setup; do not publish an APK containing or depending on a shared client-side secret.

## Milestone 10

Milestone 10 adds a textbook-grounded child Study Talk screen, parent-selectable voice styles, an activity-first Color Lab and English Sentence Builder, and a more playful child UI. Study Talk retrieves only locally prepared uploaded-book page knowledge and asks the configured AI provider to answer from that grounding. Conversation history is memory-only.

Voice presets are style controls (pitch/rate), not exact character voice cloning. The included presets are Warm Mitra, Energetic Hero, Playful Hero, and Storyteller.

## AI providers

Mitra supports two optional remote providers for prepared-textbook analysis and grounded study Q&A:

- OpenAI (default remote provider)
- Cloudflare Workers AI (free-tier option; parent supplies Cloudflare Account ID and Workers AI API token)

Built-in Standard 2 skills remain local/offline. Provider credentials are entered after install and stored separately using the app secret store.

