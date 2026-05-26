package com.voicefx.core.model

import android.net.Uri

data class VoiceNote(
    val id: String,
    val uri: Uri,
    val fileName: String,
    val durationMs: Long,
    val fileSize: Long,
    val mimeType: String,
    val source: VoiceNoteSource,
    val createdAt: Long
)

enum class VoiceNoteSource(val displayName: String) {
    RECORDING("Recording"),
    INTERNAL_STORAGE("Internal Storage"),
    SD_CARD("SD Card"),
    WHATSAPP_HISTORY("WhatsApp History");
}
