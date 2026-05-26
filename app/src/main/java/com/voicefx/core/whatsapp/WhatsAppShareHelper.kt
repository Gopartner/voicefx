package com.voicefx.core.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppShareHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.voicefx.fileprovider"
        val WHATSAPP_PACKAGES = listOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business"
        )
    }

    data class ShareResult(
        val success: Boolean,
        val message: String = ""
    )

    fun shareAsVoiceNote(uri: Uri, mimeType: String = "audio/opus"): ShareResult {
        return try {
            val whatsAppPackage = findInstalledWhatsApp()
                ?: return ShareResult(false, "WhatsApp is not installed")

            val shareUri = ensureContentUri(uri)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage(whatsAppPackage)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ShareResult(true)
            } else {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Share to WhatsApp"))
                ShareResult(true)
            }
        } catch (e: Exception) {
            ShareResult(false, e.message ?: "Unknown error")
        }
    }

    fun shareAsVoiceNoteWithPicker(uri: Uri, mimeType: String = "audio/opus"): ShareResult {
        return try {
            val shareUri = ensureContentUri(uri)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Share Voice Note"))
            ShareResult(true)
        } catch (e: Exception) {
            ShareResult(false, e.message ?: "Unknown error")
        }
    }

    fun isWhatsAppInstalled(): Boolean {
        return findInstalledWhatsApp() != null
    }

    fun findInstalledWhatsApp(): String? {
        for ((pkg, _) in WHATSAPP_PACKAGES) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: Exception) { }
        }
        return null
    }

    private fun ensureContentUri(uri: Uri): Uri {
        if (uri.scheme == "content") return uri
        val file = File(uri.path ?: return uri)
        if (!file.exists()) return uri
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
    }
}
