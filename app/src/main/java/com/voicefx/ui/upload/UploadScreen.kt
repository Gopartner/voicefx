package com.voicefx.ui.upload

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicefx.core.model.JobStatus

@Composable
fun UploadScreen(
    audioUri: String,
    presetName: String,
    onNavigateBack: () -> Unit,
    onProcessingComplete: (String) -> Unit,
    viewModel: UploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(audioUri, presetName) {
        viewModel.startUpload(Uri.parse(audioUri), presetName)
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete && uiState.resultUri != null) {
            onProcessingComplete(uiState.resultUri.toString())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            val icon = when (uiState.status) {
                JobStatus.UPLOADING -> Icons.Default.CloudUpload
                JobStatus.QUEUED -> Icons.Default.HourglassEmpty
                JobStatus.PROCESSING -> Icons.Default.Sync
                JobStatus.COMPLETED -> Icons.Default.CheckCircle
                JobStatus.FAILED -> Icons.Default.Error
                null -> Icons.Default.CloudUpload
            }

            val iconTint = when (uiState.status) {
                JobStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                JobStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = iconTint
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = when (uiState.status) {
                    JobStatus.UPLOADING -> "Uploading Audio…"
                    JobStatus.QUEUED -> "Waiting in Queue…"
                    JobStatus.PROCESSING -> "Processing Voice…"
                    JobStatus.COMPLETED -> "Processing Complete!"
                    JobStatus.FAILED -> "Processing Failed"
                    null -> "Starting…"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.status == JobStatus.PROCESSING) {
                Text(
                    text = "Your voice note is being processed on the server.\nThis usually takes 30-60 seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (uiState.status == JobStatus.UPLOADING || uiState.status == JobStatus.PROCESSING) {
                Spacer(Modifier.height(24.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val errorText = uiState.error
            if (errorText != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorText,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (uiState.status == JobStatus.FAILED) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { viewModel.startUpload(Uri.parse(audioUri), presetName) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry")
                }
            }

            if (uiState.sessionId != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Session: ${uiState.sessionId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
