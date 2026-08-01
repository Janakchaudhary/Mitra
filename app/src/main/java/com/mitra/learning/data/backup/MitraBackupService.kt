package com.mitra.learning.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import com.mitra.learning.data.db.MitraDatabase
import com.mitra.learning.settings.LearningSettings
import com.mitra.learning.settings.LearningSettingsRepository
import com.mitra.learning.voice.VoiceStyle
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Parent-triggered local backup. API credentials, PIN and raw audio are never exported.
 */
class MitraBackupService(
    context: Context,
    private val database: MitraDatabase,
    private val settingsRepository: LearningSettingsRepository,
) {
    private val appContext = context.applicationContext

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }
            appContext.contentResolver.openOutputStream(uri, "w")?.use { output ->
                ZipOutputStream(output.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry("backup_meta.txt"))
                    zip.write("MITRA_BACKUP_V2\nNo API credentials or PIN included.\n".toByteArray())
                    zip.closeEntry()
                    val settings = settingsRepository.get()
                    zip.putNextEntry(ZipEntry("learning_settings.json"))
                    zip.write(settings.toBackupJson().toByteArray())
                    zip.closeEntry()
                    addFile(zip, appContext.getDatabasePath(DATABASE_NAME), "database/$DATABASE_NAME")
                    addDirectory(zip, File(appContext.filesDir, "books"), "books")
                    addDirectory(zip, File(appContext.filesDir, "question_bank"), "question_bank")
                }
            } ?: error("Could not open backup destination")
        }
    }

    suspend fun restoreFrom(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val temp = File(appContext.cacheDir, "mitra-restore-${System.currentTimeMillis()}")
            temp.deleteRecursively()
            temp.mkdirs()
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val target = safeTarget(temp, entry.name)
                        if (entry.isDirectory) target.mkdirs() else {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { zip.copyTo(it) }
                        }
                        zip.closeEntry()
                    }
                }
            } ?: error("Could not open backup file")

            val metadata = File(temp, "backup_meta.txt").readText()
            require(metadata.startsWith("MITRA_BACKUP_V1") || metadata.startsWith("MITRA_BACKUP_V2")) {
                "This is not a Mitra backup"
            }
            File(temp, "learning_settings.json").takeIf { it.exists() }?.let { settingsFile ->
                settingsRepository.save(settingsFile.readText().toLearningSettings())
            }
            val restoredDb = File(temp, "database/$DATABASE_NAME")
            require(restoredDb.exists() && restoredDb.length() > 0) { "Backup database is missing" }

            database.close()
            val currentDb = appContext.getDatabasePath(DATABASE_NAME)
            currentDb.parentFile?.mkdirs()
            File(currentDb.path + "-wal").delete()
            File(currentDb.path + "-shm").delete()
            restoredDb.copyTo(currentDb, overwrite = true)

            restoreDirectory(File(temp, "books"), File(appContext.filesDir, "books"))
            restoreDirectory(File(temp, "question_bank"), File(appContext.filesDir, "question_bank"))
            temp.deleteRecursively()
        }
    }

    fun restartApp() {
        val launch = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ?: return
        appContext.startActivity(launch)
        Process.killProcess(Process.myPid())
    }

    private fun addDirectory(zip: ZipOutputStream, directory: File, prefix: String) {
        if (!directory.exists()) return
        directory.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(directory).invariantSeparatorsPath
            addFile(zip, file, "$prefix/$relative")
        }
    }

    private fun addFile(zip: ZipOutputStream, file: File, path: String) {
        if (!file.exists()) return
        zip.putNextEntry(ZipEntry(path))
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun safeTarget(root: File, name: String): File {
        val target = File(root, name)
        val rootPath = root.canonicalPath + File.separator
        require(target.canonicalPath.startsWith(rootPath)) { "Unsafe backup entry" }
        return target
    }

    private fun restoreDirectory(source: File, target: File) {
        target.deleteRecursively()
        if (!source.exists()) return
        source.copyRecursively(target, overwrite = true)
    }

    private fun LearningSettings.toBackupJson(): String = buildJsonObject {
        put("sessionMinutes", sessionMinutes)
        put("dailyMinutes", dailyMinutes)
        put("parentAccessMinutes", parentAccessMinutes)
        put("voiceStyle", voiceStyle.name)
    }.toString()

    private fun String.toLearningSettings(): LearningSettings {
        val root = Json.parseToJsonElement(this).let { it as? kotlinx.serialization.json.JsonObject }
            ?: error("Backup settings are invalid")
        return LearningSettings(
            sessionMinutes = root["sessionMinutes"]?.jsonPrimitive?.intOrNull ?: 20,
            dailyMinutes = root["dailyMinutes"]?.jsonPrimitive?.intOrNull ?: 30,
            parentAccessMinutes = root["parentAccessMinutes"]?.jsonPrimitive?.intOrNull ?: 5,
            voiceStyle = runCatching {
                VoiceStyle.valueOf(root["voiceStyle"]?.jsonPrimitive?.content ?: VoiceStyle.WARM.name)
            }.getOrDefault(VoiceStyle.WARM),
        ).normalized()
    }

    private companion object {
        const val DATABASE_NAME = "mitra.db"
    }
}
