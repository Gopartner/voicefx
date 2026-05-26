package com.voicefx.core.audio

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var startTimeMs: Long = 0L

    fun startRecording(outputUri: Uri): Result<Unit> {
        return try {
            stopRecording()
            val file = File(outputUri.path ?: return Result.failure(
                IllegalArgumentException("Invalid output URI")
            ))
            file.parentFile?.mkdirs()
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioBitRate(96000)
                setAudioChannels(1)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            startTimeMs = System.currentTimeMillis()
            Result.success(Unit)
        } catch (e: Exception) {
            isRecording = false
            Result.failure(e)
        }
    }

    fun stopRecording(): Result<Long> {
        return try {
            if (!isRecording) return Result.success(0L)
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            val duration = System.currentTimeMillis() - startTimeMs
            Result.success(duration)
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            Result.failure(e)
        }
    }

    fun isRecordingActive(): Boolean = isRecording

    fun getElapsedMs(): Long {
        return if (isRecording) System.currentTimeMillis() - startTimeMs else 0L
    }

    fun release() {
        stopRecording()
    }
}
