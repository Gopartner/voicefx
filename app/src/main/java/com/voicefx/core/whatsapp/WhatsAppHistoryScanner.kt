package com.voicefx.core.whatsapp

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.voicefx.core.model.VoiceNote
import com.voicefx.core.model.VoiceNoteSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppHistoryScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val WHATSAPP_PACKAGES = listOf(
            "com.whatsapp",
            "com.whatsapp.w4b"
        )

        private const val WHATSAPP_MEDIA_BASE = "Android/media"
    }

    data class ScannerConfig(
        val maxFiles: Int = 100,
        val minFileSize: Long = 1024L,
        val supportedExtensions: Set<String> = setOf("opus", "ogg", "aac", "m4a", "mp3", "wav")
    )

    fun scan(config: ScannerConfig = ScannerConfig()): List<VoiceNote> {
        val results = mutableListOf<VoiceNote>()
        val baseDir = Environment.getExternalStorageDirectory()

        for (pkg in WHATSAPP_PACKAGES) {
            val waDir = File(baseDir, "$WHATSAPP_MEDIA_BASE/$pkg")
            if (!waDir.exists()) continue

            val voiceNotes = findVoiceNotes(waDir, pkg, config)
            results.addAll(voiceNotes)

            if (results.size >= config.maxFiles) break
        }

        return results.sortedByDescending { it.createdAt }
    }

    private fun findVoiceNotes(dir: File, packageName: String, config: ScannerConfig): List<VoiceNote> {
        val results = mutableListOf<VoiceNote>()
        if (!dir.isDirectory) return results

        val voiceNoteDirs = dir.listFiles { file ->
            file.isDirectory && (
                file.name.contains("voice", ignoreCase = true) ||
                file.name.contains("audio", ignoreCase = true) ||
                file.name.contains("notes", ignoreCase = true) ||
                file.name.contains("ptt", ignoreCase = true)
            )
        } ?: emptyArray()

        for (vd in voiceNoteDirs) {
            val files = vd.listFiles { file ->
                file.isFile &&
                file.length() >= config.minFileSize &&
                config.supportedExtensions.contains(file.extension.lowercase())
            } ?: emptyArray()

            for (file in files.take(config.maxFiles - results.size)) {
                val voiceNote = VoiceNote(
                    id = "wa_${packageName}_${file.lastModified()}_${file.name.hashCode()}",
                    uri = Uri.fromFile(file),
                    fileName = file.name,
                    durationMs = estimateDuration(file),
                    fileSize = file.length(),
                    mimeType = getMimeType(file),
                    source = VoiceNoteSource.WHATSAPP_HISTORY,
                    createdAt = file.lastModified()
                )
                results.add(voiceNote)
            }

            if (results.size >= config.maxFiles) break

            val subDirs = vd.listFiles { it.isDirectory } ?: emptyArray()
            for (sub in subDirs) {
                val subFiles = sub.listFiles { file ->
                    file.isFile &&
                    file.length() >= config.minFileSize &&
                    config.supportedExtensions.contains(file.extension.lowercase())
                } ?: emptyArray()

                for (file in subFiles.take(config.maxFiles - results.size)) {
                    val voiceNote = VoiceNote(
                        id = "wa_${packageName}_${file.lastModified()}_${file.name.hashCode()}",
                        uri = Uri.fromFile(file),
                        fileName = file.name,
                        durationMs = estimateDuration(file),
                        fileSize = file.length(),
                        mimeType = getMimeType(file),
                        source = VoiceNoteSource.WHATSAPP_HISTORY,
                        createdAt = file.lastModified()
                    )
                    results.add(voiceNote)
                }
                if (results.size >= config.maxFiles) break
            }
        }

        return results
    }

    fun scanUsingMediaStore(config: ScannerConfig = ScannerConfig()): List<VoiceNote> {
        val results = mutableListOf<VoiceNote>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.DATA} LIKE ? OR ${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf(
            "%com.whatsapp%",
            "%com.whatsapp.w4b%"
        )

        try {
            context.contentResolver.query(
                collection, projection, selection, selectionArgs,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext() && results.size < config.maxFiles) {
                    val filePath = cursor.getString(dataCol)
                    val ext = filePath.substringAfterLast('.', "").lowercase()
                    if (ext !in config.supportedExtensions) continue

                    results.add(VoiceNote(
                        id = "wa_ms_${cursor.getLong(idCol)}",
                        uri = Uri.fromFile(File(filePath)),
                        fileName = cursor.getString(nameCol),
                        durationMs = cursor.getLong(durCol),
                        fileSize = cursor.getLong(sizeCol),
                        mimeType = cursor.getString(mimeCol),
                        source = VoiceNoteSource.WHATSAPP_HISTORY,
                        createdAt = cursor.getLong(dateCol) * 1000L
                    ))
                }
            }
        } catch (_: Exception) { }

        return results
    }

    private fun estimateDuration(file: File): Long {
        val size = file.length()
        return (size / 4000) * 1000L
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "opus" -> "audio/opus"
            "ogg" -> "audio/ogg"
            "aac" -> "audio/aac"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "amr" -> "audio/amr"
            "3gp" -> "audio/3gpp"
            else -> "audio/*"
        }
    }
}
