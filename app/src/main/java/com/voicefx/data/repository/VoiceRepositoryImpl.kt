package com.voicefx.data.repository

import android.content.Context
import android.net.Uri
import com.voicefx.core.audio.AudioPlayer
import com.voicefx.core.audio.AudioRecorder
import com.voicefx.core.model.RecordingState
import com.voicefx.core.model.VoiceNote
import com.voicefx.core.model.VoiceNoteSource
import com.voicefx.data.local.dao.VoiceNoteDao
import com.voicefx.data.local.entity.VoiceNoteEntity
import com.voicefx.domain.repository.VoiceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val voiceNoteDao: VoiceNoteDao
) : VoiceRepository {

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)

    override fun getRecordingState(): Flow<RecordingState> = _recordingState.asStateFlow()

    override suspend fun startRecording(outputUri: Uri): Result<Unit> {
        _recordingState.value = RecordingState.Recording
        val result = audioRecorder.startRecording(outputUri)
        if (result.isFailure) {
            _recordingState.value = RecordingState.Error(result.exceptionOrNull()?.message ?: "Recording failed")
        }
        return result
    }

    override suspend fun stopRecording(): Result<Long> {
        val result = audioRecorder.stopRecording()
        if (result.isSuccess) {
            val durationMs = result.getOrThrow()
        }
        return result
    }

    override suspend fun playAudio(uri: Uri): Result<Unit> {
        return audioPlayer.play(uri)
    }

    override suspend fun stopPlayback() {
        audioPlayer.stop()
    }

    override suspend fun deleteRecording(uri: Uri): Result<Unit> {
        return try {
            val file = File(uri.path ?: return Result.failure(IllegalArgumentException("Invalid URI")))
            if (file.exists()) file.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getVoiceNoteHistory(): Flow<List<VoiceNote>> {
        return voiceNoteDao.getAllVoiceNotes().map { entities ->
            entities.map { entity ->
                VoiceNote(
                    id = entity.id,
                    uri = Uri.parse(entity.uri),
                    fileName = entity.fileName,
                    durationMs = entity.durationMs,
                    fileSize = entity.fileSize,
                    mimeType = entity.mimeType,
                    source = try {
                        VoiceNoteSource.valueOf(entity.source)
                    } catch (_: Exception) {
                        VoiceNoteSource.RECORDING
                    },
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override suspend fun saveVoiceNote(voiceNote: VoiceNote): Result<Unit> {
        return try {
            voiceNoteDao.insert(VoiceNoteEntity(
                id = voiceNote.id,
                uri = voiceNote.uri.toString(),
                fileName = voiceNote.fileName,
                durationMs = voiceNote.durationMs,
                fileSize = voiceNote.fileSize,
                mimeType = voiceNote.mimeType,
                source = voiceNote.source.name,
                createdAt = voiceNote.createdAt
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRecordingState(state: RecordingState) {
        _recordingState.value = state
    }
}
