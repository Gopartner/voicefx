package com.voicefx.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicefx.core.model.VoicePreset
import com.voicefx.domain.usecase.GetVoicePresetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val presets: List<VoicePreset> = emptyList(),
    val selectedPreset: VoicePreset = VoicePreset.ORIGINAL,
    val showPresetMenu: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val getVoicePresetsUseCase: GetVoicePresetsUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPresets()
    }

    private fun loadPresets() {
        viewModelScope.launch {
            val presets = getVoicePresetsUseCase()
            _uiState.value = _uiState.value.copy(presets = presets)
        }
    }

    fun selectPreset(preset: VoicePreset) {
        _uiState.value = _uiState.value.copy(
            selectedPreset = preset,
            showPresetMenu = false
        )
    }

    fun togglePresetMenu() {
        _uiState.value = _uiState.value.copy(
            showPresetMenu = !_uiState.value.showPresetMenu
        )
    }

    fun dismissPresetMenu() {
        _uiState.value = _uiState.value.copy(showPresetMenu = false)
    }
}
