package com.mitra.learning.books.text

import android.content.Context
import android.graphics.Bitmap
import cz.adaptech.tesseract4android.TessBaseAPI
import java.io.Closeable
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Small serial wrapper around Tesseract. The native API is intentionally kept warm because
 * loading Gujarati + English recognition data for every page is expensive.
 */
class TesseractOcrEngine(
    context: Context,
) : Closeable {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var tess: TessBaseAPI? = null

    suspend fun recognize(bitmap: Bitmap): String = mutex.withLock {
        withContext(Dispatchers.Default) {
            val api = ensureReady()
            api.setImage(bitmap)
            api.getUTF8Text().orEmpty().normalizeRecognizedText()
        }
    }

    private fun ensureReady(): TessBaseAPI {
        tess?.let { return it }
        val dataRoot = File(appContext.filesDir, "tesseract-v2").apply { mkdirs() }
        val tessData = File(dataRoot, "tessdata").apply { mkdirs() }
        copyAssetIfNeeded("tessdata/guj.traineddata", File(tessData, "guj.traineddata"))
        copyAssetIfNeeded("tessdata/eng.traineddata", File(tessData, "eng.traineddata"))

        val created = TessBaseAPI()
        if (!created.init(dataRoot.absolutePath, "guj+eng")) {
            created.recycle()
            error("Offline OCR could not initialize Gujarati/English language data.")
        }
        tess = created
        return created
    }

    private fun copyAssetIfNeeded(assetPath: String, destination: File) {
        if (destination.exists() && destination.length() > 0L) return
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, "${destination.name}.tmp")
        temporary.delete()
        appContext.assets.open(assetPath).use { input ->
            temporary.outputStream().use { output -> input.copyTo(output) }
        }
        require(temporary.length() > 0L) { "Bundled OCR language data is empty: $assetPath" }
        destination.delete()
        if (!temporary.renameTo(destination)) {
            temporary.copyTo(destination, overwrite = true)
            temporary.delete()
        }
    }

    override fun close() {
        tess?.recycle()
        tess = null
    }
}

internal fun String.normalizeRecognizedText(): String = this
    .replace("\u0000", "")
    .replace(Regex("[ \\t]+"), " ")
    .replace(Regex(" *\\n *"), "\n")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()
