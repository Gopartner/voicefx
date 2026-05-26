package com.voicefx.domain.repository

import android.net.Uri
import com.voicefx.core.model.RecordingState
import com.voicefx.core.model.VoiceNote
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    fun getRecordingState(): Flow<RecordingState>
    suspend fun startRecording(outputUri: Uri): Result<Unit>
    suspend fun stopRecording(): Result<Long>
    suspend fun playAudio(uri: Uri): Result<Unit>
    suspend fun stopPlayback()
    suspend fun deleteRecording(uri: Uri): Result<Unit>
    fun getVoiceNoteHistory(): Flow<List<VoiceNote>>
    suspend fun saveVoiceNote(voiceNote: VoiceNote): Result<Unit>
}
