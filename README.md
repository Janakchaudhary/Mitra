# Mitra / મિત્ર

A local-first Android learning companion for one Standard 2 Gujarati-medium child.

## Current version — Milestone 6 (0.6.0)

Mitra now supports the full local-first path from PDF books to richer child learning sessions:

- Parent PIN and separate child/parent areas.
- Private PDF import, SHA-256 duplicate detection and local PDF viewer.
- Room curriculum, concepts, prerequisites, mastery, sessions and attempts.
- Gujarati push-to-talk and Gujarati TTS with text fallback.
- Parent-reviewed chapter detection and manual chapter editing.
- Real optional OpenAI textbook page analysis through a replaceable `AiGateway`.
- Book-grounded activity generation from locally cached page knowledge.
- Numeric questions, multiple choice, short text, keyword checks, riddles, reading, vocabulary, stories, book-look, drawing, physical missions, Teach-Mitra and recap activities.
- Hints that reduce mastery gain on assessed answers.
- Participation-only activities recorded without changing mastery.
- Local safety replacement for physical/drawing instructions.
- Automatic off-screen activity injection for longer sessions.
- No Firebase, Supabase, PostgreSQL, account system, browser, feed, ads or social features.

## Important Milestone 6 note

Milestone 5 marked reading/vocabulary/open-ended concepts `practiceReady=false` because only integer evaluation existed then. After upgrading to 0.6.0, use **Prepare again** on a textbook chapter to let the new analysis rules enable appropriate language/story concepts.

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

1. Upgrade over 0.5.1; do not uninstall first.
2. Confirm existing books/PIN/progress remain.
3. In Parent Mode, configure/test the AI provider if not already configured.
4. Open one real textbook chapter and press **Prepare again**.
5. Start **રમીએ**.
6. Verify mixed activities appear, including a locally assessed question and an exploration/off-screen activity.
7. Use **સંકેત** on an assessed question.
8. Try a multiple-choice activity.
9. Complete a physical/drawing/Teach-Mitra activity and confirm it advances without claiming a correct answer.
10. Say **બસ** and confirm immediate exit still works.

## Privacy/runtime boundary

Books, prepared knowledge, progress and settings remain on the phone. Remote AI receives only the parent-selected book material/context required for analysis/activity generation. Child microphone recordings, child answer transcripts, mastery and session history are not sent to the remote model.

A parent-entered API key inside a mobile APK is only appropriate for this private development/personal-use setup; do not publish an APK containing or depending on a shared client-side secret.
