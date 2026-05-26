package com.voicefx.domain.usecase

import android.net.Uri
import com.voicefx.core.model.VoicePreset
import com.voicefx.core.queue.UploadQueueManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadAndProcessUseCase @Inject constructor(
    private val uploadQueueManager: UploadQueueManager
) {
    suspend operator fun invoke(audioUri: Uri, preset: VoicePreset): UploadQueueManager.UploadResult {
        return uploadQueueManager.uploadAndProcess(audioUri, preset)
    }
}
