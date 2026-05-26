package com.voicefx.core.queue

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.voicefx.core.model.ConversionJob
import com.voicefx.core.model.JobStatus
import com.voicefx.core.model.VoicePreset
import com.voicefx.core.network.GitHubApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadQueueManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubApiService: GitHubApiService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeJobs = MutableStateFlow<List<ConversionJob>>(emptyList())
    val activeJobs = _activeJobs.asStateFlow()

    private val _currentJobStatus = MutableStateFlow<JobStatus?>(null)
    val currentJobStatus = _currentJobStatus.asStateFlow()

    data class UploadResult(
        val success: Boolean,
        val resultUri: Uri? = null,
        val sessionId: String? = null,
        val errorMessage: String? = null
    )

    suspend fun uploadAndProcess(
        audioUri: Uri,
        preset: VoicePreset
    ): UploadResult {
        if (!gitHubApiService.isConfigured()) {
            return UploadResult(false, errorMessage = "GitHub token not configured. Set it in Settings.")
        }

        val sessionId = UUID.randomUUID().toString().substring(0, 8)
        val tagName = "voicefx-$sessionId"
        val releaseName = "VoiceFX $sessionId ($preset)"

        _currentJobStatus.value = JobStatus.UPLOADING

        val job = ConversionJob(
            sessionId = sessionId,
            releaseId = 0L,
            preset = preset,
            status = JobStatus.UPLOADING,
            inputUri = audioUri.toString()
        )
        _activeJobs.value = _activeJobs.value + job

        return try {
            val audioBytes = readAudioBytes(audioUri)
                ?: return UploadResult(false, errorMessage = "Cannot read audio file")

            val extension = detectExtension(audioUri)
            val fileName = "input.$extension"

            val releaseResult = gitHubApiService.createDraftRelease(tagName, releaseName)
            if (releaseResult.isFailure) {
                return UploadResult(false, errorMessage = "Failed to create release: ${releaseResult.exceptionOrNull()?.message}")
            }
            val release = releaseResult.getOrThrow()

            val updatedJob = job.copy(releaseId = release.id, status = JobStatus.UPLOADING)
            _activeJobs.value = _activeJobs.value.map { if (it.sessionId == sessionId) updatedJob else it }
            val mimeType = when (extension) {
                "opus" -> "audio/opus"
                "ogg" -> "audio/ogg"
                "aac" -> "audio/aac"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "m4a" -> "audio/mp4"
                else -> "audio/octet-stream"
            }

            val uploadResult = gitHubApiService.uploadAsset(
                uploadUrl = release.uploadUrl,
                fileName = fileName,
                data = audioBytes,
                mimeType = mimeType
            )
            if (uploadResult.isFailure) {
                gitHubApiService.deleteRelease(release.id)
                return UploadResult(false, errorMessage = "Failed to upload audio: ${uploadResult.exceptionOrNull()?.message}")
            }

            _currentJobStatus.value = JobStatus.QUEUED

            val triggerResult = gitHubApiService.triggerWorkflow(
                eventType = "voice-convert",
                payload = mapOf(
                    "release_id" to release.id.toString(),
                    "preset" to preset.name.lowercase(),
                    "session_id" to sessionId
                )
            )
            if (triggerResult.isFailure) {
                gitHubApiService.deleteRelease(release.id)
                return UploadResult(false, errorMessage = "Failed to trigger workflow: ${triggerResult.exceptionOrNull()?.message}")
            }

            val processingJob = updatedJob.copy(status = JobStatus.QUEUED)
            _activeJobs.value = _activeJobs.value.map { if (it.sessionId == sessionId) processingJob else it }

            _currentJobStatus.value = JobStatus.PROCESSING

            val pollResult = pollForResult(release.id, sessionId)

            if (pollResult.success) {
                val completeJob = processingJob.copy(
                    status = JobStatus.COMPLETED,
                    resultUri = pollResult.resultUri?.toString(),
                    updatedAt = System.currentTimeMillis()
                )
                _activeJobs.value = _activeJobs.value.map { if (it.sessionId == sessionId) completeJob else it }
                _currentJobStatus.value = JobStatus.COMPLETED
                UploadResult(true, pollResult.resultUri, sessionId)
            } else {
                val failedJob = processingJob.copy(
                    status = JobStatus.FAILED,
                    errorMessage = pollResult.errorMessage,
                    updatedAt = System.currentTimeMillis()
                )
                _activeJobs.value = _activeJobs.value.map { if (it.sessionId == sessionId) failedJob else it }
                _currentJobStatus.value = JobStatus.FAILED
                UploadResult(false, errorMessage = pollResult.errorMessage, sessionId = sessionId)
            }
        } catch (e: Exception) {
            val failedJob = job.copy(status = JobStatus.FAILED, errorMessage = e.message, updatedAt = System.currentTimeMillis())
            _activeJobs.value = _activeJobs.value.map { if (it.sessionId == sessionId) failedJob else it }
            _currentJobStatus.value = JobStatus.FAILED
            UploadResult(false, errorMessage = e.message, sessionId = sessionId)
        }
    }

    private suspend fun pollForResult(releaseId: Long, sessionId: String, maxAttempts: Int = 60): UploadResult {
        for (attempt in 1..maxAttempts) {
            delay(5000)

            try {
                val assets = gitHubApiService.getReleaseAssets(releaseId)
                if (assets.isSuccess) {
                    val outputAsset = assets.getOrThrow().find { it.name.startsWith("output") }
                    if (outputAsset != null) {
                        val downloadResult = gitHubApiService.downloadAsset(outputAsset.downloadUrl)
                        if (downloadResult.isSuccess) {
                            val cacheDir = File(context.cacheDir, "voicefx_results")
                            cacheDir.mkdirs()
                            val resultFile = File(cacheDir, "result_${sessionId}.ogg")
                            resultFile.writeBytes(downloadResult.getOrThrow())

                            gitHubApiService.deleteRelease(releaseId)

                            return UploadResult(true, resultUri = Uri.fromFile(resultFile), sessionId = sessionId)
                        }
                    }
                }

                val status = gitHubApiService.getWorkflowRunStatus(sessionId)
                if (status.isSuccess && status.getOrThrow() == "failure") {
                    gitHubApiService.deleteRelease(releaseId)
                    return UploadResult(false, errorMessage = "Workflow execution failed", sessionId = sessionId)
                }
            } catch (e: Exception) {
                if (attempt >= maxAttempts) {
                    return UploadResult(false, errorMessage = "Timed out waiting for result", sessionId = sessionId)
                }
            }
        }
        return UploadResult(false, errorMessage = "Timed out after ${maxAttempts * 5} seconds", sessionId = sessionId)
    }

    fun clearJobs() {
        _activeJobs.value = emptyList()
        _currentJobStatus.value = null
    }

    private fun readAudioBytes(uri: Uri): ByteArray? {
        return try {
            if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } else {
                File(uri.path ?: return null).let { file ->
                    if (file.exists()) file.readBytes() else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun detectExtension(uri: Uri): String {
        if (uri.scheme == "content") {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val name = it.getString(nameIndex)
                            val ext = name.substringAfterLast('.', "").lowercase()
                            if (ext.isNotEmpty()) return ext
                        }
                    }
                }
            } catch (_: Exception) { }
        } else {
            val path = uri.path ?: return "aac"
            val ext = path.substringAfterLast('.', "").lowercase()
            if (ext.isNotEmpty()) return ext
        }
        return "aac"
    }
}
