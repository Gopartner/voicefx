package com.voicefx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val fileName: String,
    val durationMs: Long,
    val fileSize: Long,
    val mimeType: String,
    val source: String,
    val createdAt: Long
)
