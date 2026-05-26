package com.voicefx.domain.usecase

import com.voicefx.core.model.VoiceNote
import com.voicefx.domain.repository.WhatsAppRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanWhatsAppHistoryUseCase @Inject constructor(
    private val whatsAppRepository: WhatsAppRepository
) {
    suspend operator fun invoke(): List<VoiceNote> {
        return whatsAppRepository.scanHistory()
    }
}
