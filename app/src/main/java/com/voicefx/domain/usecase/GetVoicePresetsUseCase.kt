package com.voicefx.domain.usecase

import com.voicefx.core.model.VoicePreset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetVoicePresetsUseCase @Inject constructor() {
    operator fun invoke(): List<VoicePreset> {
        return VoicePreset.entries.toList()
    }

    fun getDefault(): VoicePreset {
        return VoicePreset.default()
    }
}
