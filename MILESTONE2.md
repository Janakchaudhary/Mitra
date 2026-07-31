# Milestone 2 release notes

Version: 0.2.0

## Added
- Room schema version 2 with non-destructive migration from version 1.
- Concepts, prerequisites, mastery, sessions and attempts.
- Built-in Standard 2 maths practice curriculum used only as local scaffolding.
- Deterministic concept selection and mastery policy.
- Gujarati/English number input normalization.
- `AiGateway` abstraction with fully local `MockAiGateway`.
- Child text practice session launched from **રમીએ**.
- Session stop, skip, completion and persisted progress.
- Unit tests for the new learning logic.

## Intentionally not included
- Microphone / speech recognition.
- Text-to-speech.
- Remote AI/API keys.
- PDF chapter analysis.
- Parent progress dashboard.

These belong to later milestones so the learning engine remains testable independently.
