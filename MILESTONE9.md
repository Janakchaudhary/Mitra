# Milestone 9 — Standard 2 Skill Engine

Version: 0.9.0 (versionCode 12)

## Added

- Dedicated child **કૌશલ્ય રમત** route for offline Standard 2 drills.
- Two-digit addition: 2d+1d, 2d+2d without carry, and carrying.
- Two-digit subtraction: 2d-1d, 2d-2d without borrowing, and borrowing.
- Missing-number, greater/smaller and arithmetic word-problem concepts.
- Multiplication as equal groups/repeated addition.
- Separate tables 2,3,4,5,6,7,8,9,10 mastery concepts.
- Gujarati word recognition, spelling, missing letter, read aloud, sentence completion, word meaning and singular/plural.
- English word recognition, spelling, missing letter, read aloud and sentence completion.
- `spokenPromptGujarati` allows dictation without showing the answer.
- Gujarati/English speech language tags keep Gujarati activities on `gu-IN` and English dictation/read-aloud on `en-IN` when Android voices are available.
- Local number normalization accepts Gujarati digits, English digits and common Gujarati number words through 100 for spoken maths/table answers.
- Built-in skill generation is local/deterministic even when OpenAI is configured.
- Parent progress includes all Standard 2 built-in skills with independent mastery.
- OpenAI chapter analysis is prompted to keep independently teachable skills separate.

## Database

No Room migration. New built-in concepts are seeded into the existing `concepts` and `concept_prerequisites` tables on first session after upgrade. Existing books and mastery remain intact.

## Validation

Run:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```
