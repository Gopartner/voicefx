package com.voicefx.data.repository

import android.net.Uri
import com.voicefx.core.model.VoiceNote
import com.voicefx.core.whatsapp.WhatsAppHistoryScanner
import com.voicefx.core.whatsapp.WhatsAppShareHelper
import com.voicefx.domain.repository.WhatsAppRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppRepositoryImpl @Inject constructor(
    private val historyScanner: WhatsAppHistoryScanner,
    private val shareHelper: WhatsAppShareHelper
) : WhatsAppRepository {

    override suspend fun scanHistory(): List<VoiceNote> {
        val direct = historyScanner.scan()
        if (direct.isNotEmpty()) return direct
        return historyScanner.scanUsingMediaStore()
    }

    override suspend fun shareAsVoiceNote(uri: Uri, mimeType: String): Result<Unit> {
        val result = shareHelper.shareAsVoiceNote(uri, mimeType)
        return if (result.success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(result.message))
        }
    }

    override suspend fun shareWithPicker(uri: Uri, mimeType: String): Result<Unit> {
        val result = shareHelper.shareAsVoiceNoteWithPicker(uri, mimeType)
        return if (result.success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(result.message))
        }
    }

    override fun isWhatsAppInstalled(packageName: String): Boolean {
        return shareHelper.findInstalledWhatsApp() == packageName ||
               shareHelper.isWhatsAppInstalled()
    }

    override fun getWhatsAppPackages(): List<String> {
        return WhatsAppShareHelper.WHATSAPP_PACKAGES.map { it.first }
    }
}
