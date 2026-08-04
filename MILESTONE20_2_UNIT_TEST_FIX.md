# Mitra 0.20.2 — Unit-test and runtime reliability fix

- Prevents speculative front-page OCR scans when preparing a single manually entered chapter.
- Automatic printed-page range repair now requires at least three existing generic chapters.
- Adds a TTS-independent fallback for moving to the next question after a correct answer.
- The normal speech completion callback remains primary; the fallback only runs when a vendor TTS callback is missing or conflated.
- Fixes `BookPreparationCapabilityTest.offlineTextPreparationUsesExtractorWithoutRenderingPdf`.
- Fixes `LearningSessionVoiceTest.recognized voice answer is checked and advances automatically after praise`.
- Version updated to 0.20.2 (versionCode 40).
