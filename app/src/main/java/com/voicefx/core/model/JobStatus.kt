package com.voicefx.core.model

data class ConversionJob(
    val sessionId: String,
    val releaseId: Long,
    val preset: VoicePreset,
    val status: JobStatus,
    val inputUri: String,
    val resultUri: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class JobStatus(val displayName: String) {
    UPLOADING("Uploading"),
    QUEUED("Queued"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed");
}
