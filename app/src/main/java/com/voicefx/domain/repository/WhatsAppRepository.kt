package com.voicefx.domain.repository

import android.net.Uri
import com.voicefx.core.model.VoiceNote

interface WhatsAppRepository {
    suspend fun scanHistory(): List<VoiceNote>
    suspend fun shareAsVoiceNote(uri: Uri, mimeType: String): Result<Unit>
    suspend fun shareWithPicker(uri: Uri, mimeType: String): Result<Unit>
    fun isWhatsAppInstalled(packageName: String): Boolean
    fun getWhatsAppPackages(): List<String>
}
