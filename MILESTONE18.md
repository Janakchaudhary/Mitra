# Milestone 18 — Offline AI Book Preparation

## Implemented

- Added text-analysis capabilities alongside existing image-analysis capabilities.
- Offline Local can detect chapter structure from selected contents/index pages.
- Added on-device embedded PDF text extraction with PDFBox Android.
- Added Gujarati + English OCR fallback for scanned/image-only PDF pages.
- Bundled `guj.traineddata` and `eng.traineddata` under `app/src/main/assets/tessdata`.
- Added a serialized, warm Tesseract OCR engine using app-private language-data files.
- Reworked `OfflineAiGateway` to prepare table-of-contents and chapter metadata from extracted text.
- Added strict JSON parsing and validation for an optional parent-imported LiteRT-LM model.
- Added deterministic chapter/page/concept fallback when no model is installed or model output is invalid.
- Preserved the existing OpenAI/Cloudflare image-analysis path.
- Added **Prepare all chapters** with sequential progress and per-chapter failure reporting.
- Kept manual chapter entry and parent review.
- Preserved `READY` state and cached knowledge after a failed re-prepare.
- Added capability, service-path, and rule-based contents-parser unit tests.

## Offline preparation behavior

1. Embedded PDF text is attempted first.
2. OCR is used only for pages without enough useful embedded text.
3. Picture-only pages are retained with an unreadable-page summary instead of failing the chapter.
4. Chapter pages are passed to Offline Local in batches of four.
5. LiteRT-LM is used when a compatible model is installed.
6. A conservative deterministic fallback keeps preparation available without a model.
7. All resulting knowledge stays on the device.

## Limitations

- OCR quality depends on scan clarity, rotation, contrast, typography, and page layout.
- Handwriting and complex diagrams are not semantically interpreted.
- The LiteRT-LM integration is text-only; OCR provides its input for scanned pages.
- Printed textbook page numbers can be offset from PDF page numbers, so detected chapter ranges require parent review.
- Preparation is intentionally sequential rather than parallel to control memory use.

## Release

- `versionCode = 33`
- `versionName = 0.18.0`
- Room schema version `5`
- No database migration required

## Validation performed in this delivery

- Rule-based Gujarati/English contents parsing was compiled and exercised with Gujarati digits, dot leaders, administrative rows, and out-of-range pages.
- `OfflineAiGateway`, `BookPreparationService`, `AndroidOfflinePageTextExtractor`, and `TesseractOcrEngine` passed standalone Kotlin type/syntax checks with project-compatible API stubs.
- Bundled OCR data checksums match `THIRD_PARTY_NOTICES.md`.
- A complete Android Gradle/APK build was not run in the delivery environment because the source archive does not contain a Gradle wrapper and no Android SDK is installed there. The repository's GitHub Actions build remains the intended full Android validation path.
