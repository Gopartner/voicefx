package com.voicefx.ui.preview

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicefx.core.audio.AudioPlayer
import com.voicefx.domain.repository.WhatsAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreviewUiState(
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val shareState: ShareState = ShareState.IDLE
)

enum class ShareState {
    IDLE,
    SHARING,
    SUCCESS,
    ERROR
}

@HiltViewModel
class PreviewViewModel @Inject constructor(
    application: Application,
    private val audioPlayer: AudioPlayer,
    private val whatsAppRepository: WhatsAppRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState = _uiState.asStateFlow()

    private var progressJob: Job? = null

    fun playPause(uri: Uri) {
        viewModelScope.launch {
            if (audioPlayer.isCurrentlyPlaying()) {
                audioPlayer.pause()
                _uiState.value = _uiState.value.copy(isPlaying = false)
            } else {
                val result = audioPlayer.play(uri)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isPlaying = true,
                        duration = audioPlayer.getDuration()
                    )
                    startProgressTracking()
                }
            }
        }
    }

    fun stopPlayback() {
        audioPlayer.stop()
        progressJob?.cancel()
        _uiState.value = _uiState.value.copy(isPlaying = false, progress = 0f)
    }

    fun shareToWhatsApp(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(shareState = ShareState.SHARING)
            try {
                val result = whatsAppRepository.shareAsVoiceNote(uri, "audio/ogg")
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(shareState = ShareState.SUCCESS)
                } else {
                    whatsAppRepository.shareWithPicker(uri, "audio/*")
                    _uiState.value = _uiState.value.copy(shareState = ShareState.SUCCESS)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(shareState = ShareState.ERROR)
            }

            delay(2000)
            _uiState.value = _uiState.value.copy(shareState = ShareState.IDLE)
        }
    }

    fun shareWithPicker(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(shareState = ShareState.SHARING)
            try {
                whatsAppRepository.shareWithPicker(uri, "audio/ogg")
                _uiState.value = _uiState.value.copy(shareState = ShareState.SUCCESS)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(shareState = ShareState.ERROR)
            }
            delay(2000)
            _uiState.value = _uiState.value.copy(shareState = ShareState.IDLE)
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val duration = audioPlayer.getDuration()
                val position = audioPlayer.getCurrentPosition()
                val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                _uiState.value = _uiState.value.copy(
                    progress = progress,
                    currentPosition = position,
                    duration = duration
                )
                delay(100)
                if (!audioPlayer.isCurrentlyPlaying()) {
                    _uiState.value = _uiState.value.copy(isPlaying = false)
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
