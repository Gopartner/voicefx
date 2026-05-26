package com.voicefx.core.audio

import android.net.Uri
import com.voicefx.core.model.VoicePreset

interface VoiceProcessor {
    suspend fun process(inputUri: Uri, preset: VoicePreset): Result<Uri>
}
