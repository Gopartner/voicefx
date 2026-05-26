package com.voicefx.ui.recorder

import android.app.Application
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicefx.core.model.RecordingState
import com.voicefx.domain.usecase.RecordVoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecorderUiState(
    val recordingState: RecordingState = RecordingState.Idle,
    val elapsedMs: Long = 0L,
    val outputUri: Uri? = null
)

@HiltViewModel
class RecorderViewModel @Inject constructor(
    application: Application,
    private val recordVoiceUseCase: RecordVoiceUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var startTimeMs: Long = 0L

    fun startRecording() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(recordingState = RecordingState.Recording)
            startTimeMs = System.currentTimeMillis()

            val result = recordVoiceUseCase()
            if (result.isSuccess) {
                val uri = result.getOrThrow()
                _uiState.value = _uiState.value.copy(outputUri = uri)
                startTimer()
            } else {
                _uiState.value = _uiState.value.copy(
                    recordingState = RecordingState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to start recording"
                    )
                )
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            timerJob?.cancel()
            val durationResult = recordVoiceUseCase.stop()
            val durationMs = durationResult.getOrDefault(0L)
            _uiState.value = _uiState.value.copy(
                recordingState = RecordingState.Recorded(
                    uri = _uiState.value.outputUri ?: return@launch,
                    durationMs = durationMs
                ),
                elapsedMs = durationMs
            )
        }
    }

    fun onRecordingComplete(uri: Uri, durationMs: Long) {
        _uiState.value = _uiState.value.copy(
            recordingState = RecordingState.Recorded(uri = uri, durationMs = durationMs),
            elapsedMs = durationMs
        )
    }

    fun reset() {
        timerJob?.cancel()
        val currentUri = _uiState.value.outputUri
        if (currentUri != null) {
            viewModelScope.launch {
                val file = File(currentUri.path ?: return@launch)
                if (file.exists()) file.delete()
            }
        }
        _uiState.value = RecorderUiState()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                _uiState.value = _uiState.value.copy(
                    elapsedMs = System.currentTimeMillis() - startTimeMs
                )
                delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
