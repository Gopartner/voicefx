package com.voicefx.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    data class SettingsState(
        val overlayEnabled: Boolean = true,
        val autoStartOverlay: Boolean = true,
        val locationEnabled: Boolean = false,
        val highQualityAudio: Boolean = true,
        val cacheSize: String = "0 MB",
        val appVersion: String = "1.0.0"
    )

    fun loadState(): SettingsState = SettingsState()
}
