# Mitra 0.18.1 — Tesseract OCR compile fix

## Fixed

GitHub Actions failed to compile `TesseractOcrEngine.kt` because the class was imported from the Maven group/package name:

```kotlin
import cz.adaptech.tesseract4android.TessBaseAPI
```

The Tesseract4Android library keeps the original Java API namespace. The correct import is:

```kotlin
import com.googlecode.tesseract.android.TessBaseAPI
```

This resolves `TessBaseAPI`, `setImage`, `getUTF8Text`, and `recycle` compilation errors.

## Version

- `versionCode`: 34
- `versionName`: 0.18.1
