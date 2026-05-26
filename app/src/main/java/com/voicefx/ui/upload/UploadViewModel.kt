package com.voicefx.ui.upload

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicefx.core.model.JobStatus
import com.voicefx.core.model.VoicePreset
import com.voicefx.core.queue.UploadQueueManager
import com.voicefx.domain.usecase.UploadAndProcessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val status: JobStatus? = null,
    val progress: Float = 0f,
    val resultUri: Uri? = null,
    val error: String? = null,
    val isComplete: Boolean = false,
    val sessionId: String? = null
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    application: Application,
    private val uploadAndProcessUseCase: UploadAndProcessUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState = _uiState.asStateFlow()

    fun startUpload(audioUri: Uri, presetName: String) {
        viewModelScope.launch {
            val preset = VoicePreset.fromDisplayName(presetName)

            if (preset == VoicePreset.ORIGINAL) {
                _uiState.value = UploadUiState(
                    status = JobStatus.COMPLETED,
                    resultUri = audioUri,
                    isComplete = true
                )
                return@launch
            }

            _uiState.value = UploadUiState(status = JobStatus.UPLOADING, progress = 0.1f)

            val result = uploadAndProcessUseCase(audioUri, preset)

            if (result.success && result.resultUri != null) {
                _uiState.value = UploadUiState(
                    status = JobStatus.COMPLETED,
                    resultUri = result.resultUri,
                    isComplete = true,
                    sessionId = result.sessionId
                )
            } else {
                _uiState.value = UploadUiState(
                    status = JobStatus.FAILED,
                    error = result.errorMessage ?: "Unknown error",
                    isComplete = true,
                    sessionId = result.sessionId
                )
            }
        }
    }
}
