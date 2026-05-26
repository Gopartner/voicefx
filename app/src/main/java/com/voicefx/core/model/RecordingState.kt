package com.voicefx.core.model

import android.net.Uri

sealed interface RecordingState {
    data object Idle : RecordingState
    data object Recording : RecordingState
    data class Recorded(val uri: Uri, val durationMs: Long) : RecordingState
    data class Error(val message: String) : RecordingState
}
