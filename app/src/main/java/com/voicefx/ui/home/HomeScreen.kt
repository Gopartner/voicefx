package com.voicefx.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicefx.core.model.VoicePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPicker: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToRecorder: (String) -> Unit,
    onNavigateToUpload: (String, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("VoiceFX", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { VoicePresetSelector(uiState, viewModel) }

            item { Spacer(Modifier.height(8.dp)) }

            item { SectionLabel("Record") }
            item {
                ActionCard(
                    icon = Icons.Default.Mic,
                    title = "Record Voice",
                    subtitle = "Record audio with selected voice preset",
                    onClick = { onNavigateToRecorder(uiState.selectedPreset.name) }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionLabel("Pick Audio") }
            item {
                ActionCard(
                    icon = Icons.Default.Folder,
                    title = "Internal Storage",
                    subtitle = "Browse audio files on device",
                    onClick = { onNavigateToPicker("internal") }
                )
            }
            item {
                ActionCard(
                    icon = Icons.Default.SdStorage,
                    title = "SD Card",
                    subtitle = "Browse audio files on SD card",
                    onClick = { onNavigateToPicker("sd_card") }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionLabel("WhatsApp") }
            item {
                ActionCard(
                    icon = Icons.Default.History,
                    title = "WhatsApp Voice Notes",
                    subtitle = "Select from voice note history",
                    onClick = onNavigateToHistory
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoicePresetSelector(
    uiState: HomeUiState,
    viewModel: HomeViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Voice Preset",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = uiState.showPresetMenu,
                onExpandedChange = { viewModel.togglePresetMenu() }
            ) {
                OutlinedTextField(
                    value = uiState.selectedPreset.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.showPresetMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = uiState.showPresetMenu,
                    onDismissRequest = { viewModel.dismissPresetMenu() }
                ) {
                    uiState.presets.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(preset.displayName)
                                    if (preset.isDefault) {
                                        Spacer(Modifier.width(8.dp))
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Default", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                }
                            },
                            onClick = { viewModel.selectPreset(preset) }
                        )
                    }
                }
            }

            if (uiState.selectedPreset != VoicePreset.ORIGINAL) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Non-original presets will be processed on the server (requires internet & GitHub token)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
