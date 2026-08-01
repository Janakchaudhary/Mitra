package com.mitra.learning.ai.local

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.Closeable
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Loads one parent-imported LiteRT-LM model and keeps the engine warm between short turns. */
class LiteRtLocalModel(
    private val context: Context,
    private val store: LocalModelStore,
) : Closeable {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var loadedPath: String? = null
    private var loadedStamp: Long = Long.MIN_VALUE

    suspend fun generate(systemInstruction: String, prompt: String): String = mutex.withLock {
        withContext(Dispatchers.Default) {
            val activeEngine = ensureEngine()
            activeEngine.createConversation().use { conversation ->
                conversation.sendMessage("$systemInstruction\n\n$prompt").text.trim()
                    .takeIf { it.isNotBlank() }
                    ?: error("The local model returned an empty answer.")
            }
        }
    }

    suspend fun test(): String {
        val answer = generate(
            systemInstruction = "Reply briefly and safely.",
            prompt = "Reply with only OK",
        )
        return "On-device model is ready (${store.modelSizeBytes().toHumanSize()}). Response: ${answer.take(40)}"
    }

    private suspend fun ensureEngine(): Engine {
        val path = store.modelPathOrNull()
            ?: error("Import a compatible .litertlm model in Parent settings first.")
        val modelFile = File(path)
        val stamp = modelFile.lastModified() xor modelFile.length()
        if (engine != null && loadedPath == path && loadedStamp == stamp) return engine!!
        closeEngine()

        val cache = File(context.cacheDir, "litertlm").apply { mkdirs() }
        val created = runCatching {
            Engine(
                EngineConfig(
                    modelPath = path,
                    backend = Backend.GPU(),
                    cacheDir = cache.absolutePath,
                )
            ).also { it.initialize() }
        }.getOrElse {
            Engine(
                EngineConfig(
                    modelPath = path,
                    backend = Backend.CPU(),
                    cacheDir = cache.absolutePath,
                )
            ).also { it.initialize() }
        }
        engine = created
        loadedPath = path
        loadedStamp = stamp
        return created
    }

    private fun closeEngine() {
        runCatching { engine?.close() }
        engine = null
        loadedPath = null
        loadedStamp = Long.MIN_VALUE
    }

    override fun close() = closeEngine()
}

internal fun Long.toHumanSize(): String = when {
    this >= 1024L * 1024L * 1024L -> "%.1f GB".format(this / (1024.0 * 1024.0 * 1024.0))
    this >= 1024L * 1024L -> "%.0f MB".format(this / (1024.0 * 1024.0))
    else -> "$this bytes"
}
