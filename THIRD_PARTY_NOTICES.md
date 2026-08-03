# Third-party notices — Milestone 18 offline preparation

Mitra uses the following components for on-device book preparation. Consult each upstream project/distribution for the complete license text and attribution requirements.

## PDFBox Android

- Maven coordinate: `com.tom-roush:pdfbox-android:2.0.27.0`
- Purpose: extract selectable text from requested PDF pages.
- Upstream project: PdfBox-Android, based on Apache PDFBox.
- License: Apache License 2.0.

## Tesseract4Android

- Maven coordinate: `cz.adaptech.tesseract4android:tesseract4android:4.9.0`
- Purpose: Android wrapper/native runtime for on-device OCR.
- License: Apache License 2.0.

## Tesseract language data

Bundled APK assets:

- `app/src/main/assets/tessdata/eng.traineddata`
  - SHA-256: `7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2`
- `app/src/main/assets/tessdata/guj.traineddata`
  - SHA-256: `fa69658614b4946a9afae8853d67e0689838803dfa3d12c2e35ec53ee6f8df34`

Purpose: English and Gujarati recognition for scanned textbook pages.

The language data is distributed by the Tesseract OCR project under the Apache License 2.0. These files are copied to an app-private `tessdata` directory on first OCR use; they are not downloaded at runtime.
