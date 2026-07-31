# Milestone 5 — Real textbook AI + grounded numeric practice

Version: **0.5.0**

Milestone 5 replaces the hard-wired mock provider with a configurable AI gateway while keeping the app local-first.

## Added

- Parent-only AI provider settings.
- OpenAI Responses API adapter using HTTPS/OkHttp.
- Parent-entered API key stored encrypted using an AES/GCM key protected by Android Keystore.
- Structured Outputs (`json_schema`) for:
  - contents/index detection,
  - textbook page/chapter analysis,
  - grounded numeric practice question generation.
- Vision input using rendered PDF-page JPEGs.
- `store=false` on Responses API requests.
- Retry handling for 429 and 5xx responses.
- Test-connection action using `/v1/models`.
- Dynamic `ConfigurableAiGateway`: mock/offline or OpenAI without changing the learning engine.
- Prepared page summaries/text are used as grounding when generating questions from book-derived concepts.
- Child microphone transcripts and submitted answers remain local in Milestone 5.
- Built-in math falls back to local mock questions when remote AI is unavailable.
- Book-derived concepts do not silently fall back to unrelated mock questions.
- Fixed concept selection so `practiceReady=false` concepts are never chosen simply because nothing else is ready.

## Current practice limitation

The current learning engine evaluates integer answers locally. Therefore AI chapter analysis marks `practiceReady=true` only for concepts that can be practiced with a one-integer answer, such as counting, before/after, addition, subtraction, and quantity questions.

Gujarati reading/vocabulary concepts are extracted and stored, but remain `practiceReady=false` until a later milestone adds open-ended and choice-based evaluation.

## Personal-development credential warning

OpenAI recommends that API keys not be deployed in mobile apps. This personal build supports a parent-entered key only as a development/personal-use compromise. The key is never compiled into the APK or repository, but a credential on a client device cannot be treated as fully secret.

## Parent flow

1. Parent → AI provider settings.
2. Enable OpenAI.
3. Enter API base URL, model, and API key.
4. Save and Test connection.
5. Add/open a textbook.
6. Select contents pages and detect chapters.
7. Review/edit chapter ranges.
8. Prepare a chapter.
9. Page knowledge and concepts are saved locally.
10. Numeric-ready concepts can be selected by the existing learning engine.
