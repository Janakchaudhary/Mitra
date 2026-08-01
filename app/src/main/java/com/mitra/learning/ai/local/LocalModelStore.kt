package com.mitra.learning.ai.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Stores a parent-selected LiteRT-LM model in app-private storage.
 *
 * The app intentionally does not bundle a multi-gigabyte model inside the APK. A parent can
 * import a compatible `.litertlm` model once; after that, study chat can run without internet.
 */
class LocalModelStore(private val context: Context) {
    private val modelDir = File(context.filesDir, "local_ai")
    private val modelFile = File(modelDir, "mitra-local.litertlm")

    fun modelPathOrNull(): String? = modelFile.takeIf { it.isFile && it.length() > MIN_MODEL_BYTES }?.absolutePath

    fun hasModel(): Boolean = modelPathOrNull() != null

    fun modelSizeBytes(): Long = modelFile.takeIf { it.isFile }?.length() ?: 0L

    suspend fun importFrom(uri: Uri): Long = withContext(Dispatchers.IO) {
        modelDir.mkdirs()
        val temp = File(modelDir, "mitra-local.litertlm.partial")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().buffered().use { output -> input.copyTo(output) }
            } ?: error("Could not open the selected model file.")
            require(temp.length() > MIN_MODEL_BYTES) {
                "The selected file is too small to be a LiteRT-LM model. Select a .litertlm model file."
            }
            if (modelFile.exists() && !modelFile.delete()) error("Could not replace the existing local model.")
            if (!temp.renameTo(modelFile)) {
                temp.copyTo(modelFile, overwrite = true)
                temp.delete()
            }
            modelFile.length()
        }.getOrElse { failure ->
            temp.delete()
            throw failure
        }
    }

    fun remove() {
        modelFile.delete()
        File(modelDir, "mitra-local.litertlm.partial").delete()
        File(context.cacheDir, "litertlm").deleteRecursively()
    }

    companion object {
        // This only rejects accidental tiny/non-model files. Small quantized models still pass.
        private const val MIN_MODEL_BYTES = 10L * 1024L * 1024L
    }
}
