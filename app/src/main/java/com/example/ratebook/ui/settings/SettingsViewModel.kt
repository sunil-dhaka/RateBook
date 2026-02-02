package com.example.ratebook.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ratebook.data.backup.BackupCounts
import com.example.ratebook.data.backup.BackupManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events

    fun exportBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Exporting..."

            val result = backupManager.createBackup()

            _isLoading.value = false
            _statusMessage.value = null

            result.fold(
                onSuccess = { file ->
                    _events.emit(SettingsEvent.ExportSuccess(file))
                },
                onFailure = { error ->
                    _events.emit(SettingsEvent.Error(error.message ?: "Export failed"))
                }
            )
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Importing..."

            val result = backupManager.importBackup(uri)

            _isLoading.value = false
            _statusMessage.value = null

            result.fold(
                onSuccess = { counts ->
                    _events.emit(SettingsEvent.ImportSuccess(counts))
                },
                onFailure = { error ->
                    _events.emit(SettingsEvent.Error(error.message ?: "Import failed"))
                }
            )
        }
    }

    sealed class SettingsEvent {
        data class ExportSuccess(val file: File) : SettingsEvent()
        data class ImportSuccess(val counts: BackupCounts) : SettingsEvent()
        data class Error(val message: String) : SettingsEvent()
    }

    class Factory(private val backupManager: BackupManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(backupManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
