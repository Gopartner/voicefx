package com.voicefx.core.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilePickerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val SUPPORTED_MIME_TYPES = arrayOf(
            "audio/wav",
            "audio/x-wav",
            "audio/ogg",
            "audio/opus",
            "audio/aac",
            "audio/mp4",
            "audio/mpeg",
            "audio/mp3",
            "audio/x-m4a",
            "audio/amr",
            "audio/3gpp"
        )

        val SUPPORTED_EXTENSIONS = listOf("wav", "opus", "ogg", "aac", "mp3", "m4a", "mp4", "amr", "3gp")
    }

    data class FileInfo(
        val uri: Uri,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String
    )

    fun getFileInfo(uri: Uri): FileInfo? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else "unknown"
                    val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                    val mime = context.contentResolver.getType(uri) ?: "audio/*"
                    FileInfo(uri, name, size, mime)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    fun isSupportedFile(fileName: String): Boolean {
        val ext = getFileExtension(fileName)
        return ext in SUPPORTED_EXTENSIONS
    }

    fun readBytes(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    fun copyToCache(uri: Uri, targetName: String): Uri? {
        return try {
            val cacheDir = java.io.File(context.cacheDir, "voicefx_picker")
            cacheDir.mkdirs()
            val targetFile = java.io.File(cacheDir, targetName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            null
        }
    }
}
