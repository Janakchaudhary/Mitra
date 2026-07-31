# Milestone 10 — Study Talk + Activity-first UI

Version: 0.10.0 (versionCode 13)

## Added

- Child `મિત્રને પૂછો` study conversation screen.
- Voice or text questions in Gujarati.
- Answers are grounded only in locally prepared uploaded textbook page knowledge.
- Local keyword retrieval selects relevant prepared pages; only selected snippets are sent to the configured remote AI provider.
- The model is instructed to say the answer is not available when prepared textbook grounding is insufficient.
- Study chat is kept in memory and is not stored in Room.
- Source labels are shown under grounded answers.

## Voice styles

Parent settings now include:

- Warm Mitra
- Energetic Hero
- Playful Hero
- Storyteller

These are Android TTS pitch/rate presets. They intentionally do not clone or claim to reproduce Goku, Chhota Bheem, or another copyrighted character voice. A future licensed/custom TTS implementation can plug into `SpeechOutput`.

## Activity-first learning

New `રમતથી શીખો` hub:

### Color Lab
1. Child fills a plain balloon drawing with a chosen color.
2. Identifies the color in Gujarati.
3. Identifies the color in English.
4. Types the English spelling.

### English Sentence Builder
Child taps word tiles to build age-appropriate sentences using:

- this
- that
- and
- is
- are
- a
- an

Examples include `This is a red ball.` and `That is an apple.`.

## UI

- New pastel child theme.
- Large child home action cards.
- Dedicated Study Talk and activity screens.
- Normal learning sessions now show an activity card, activity emoji, page badge, and progress bar.

## Storage / migration

- Room remains version 3.
- No Room migration is required from Milestone 9.
- Voice style is stored in DataStore.
- Study conversation history is not persisted.
