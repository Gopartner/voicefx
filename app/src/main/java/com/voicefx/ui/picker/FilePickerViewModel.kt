package com.voicefx.ui.picker

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicefx.core.storage.FilePickerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilePickerUiState(
    val selectedFileUri: Uri? = null,
    val fileName: String = "",
    val fileSize: String = "",
    val isSupported: Boolean = false
)

@HiltViewModel
class FilePickerViewModel @Inject constructor(
    application: Application,
    private val filePickerHelper: FilePickerHelper
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FilePickerUiState())
    val uiState = _uiState.asStateFlow()

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            val info = filePickerHelper.getFileInfo(uri)
            if (info != null) {
                val ext = filePickerHelper.getFileExtension(info.fileName)
                val supported = filePickerHelper.isSupportedFile(info.fileName)
                _uiState.value = FilePickerUiState(
                    selectedFileUri = uri,
                    fileName = info.fileName,
                    fileSize = formatSize(info.fileSize),
                    isSupported = supported
                )
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
        }
    }
}
