package com.voicefx.domain.usecase

import android.content.Context
import android.net.Uri
import com.voicefx.domain.repository.VoiceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordVoiceUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceRepository: VoiceRepository
) {
    private var currentUri: Uri? = null

    suspend operator fun invoke(): Result<Uri> {
        return try {
            val fileName = "voicefx_recording_${System.currentTimeMillis()}.aac"
            val outputFile = File(context.cacheDir, "voicefx_recordings/$fileName")
            outputFile.parentFile?.mkdirs()
            val uri = Uri.fromFile(outputFile)
            currentUri = uri
            voiceRepository.startRecording(uri)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stop(): Result<Long> {
        return voiceRepository.stopRecording()
    }

    fun getCurrentRecordingUri(): Uri? = currentUri
}
