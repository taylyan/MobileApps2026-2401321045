
package com.huntersstory.app.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huntersstory.app.data.repository.CloudBackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CloudSyncViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: CloudBackupRepository =
        CloudBackupRepository(application)
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<SyncState>(
        SyncState.Idle
    )

    val state = _state.asStateFlow()

    fun upload(uid: String) {
        if (_state.value is SyncState.Loading) return

        viewModelScope.launch {
            _state.value = SyncState.Loading

            try {
                repository.upload(uid)

                _state.value = SyncState.Success(
                    "Cloud backup uploaded successfully!"
                )
            } catch (e: Exception) {
                _state.value = SyncState.Error(
                    e.localizedMessage ?: "Upload failed."
                )
            }
        }
    }

    fun restore(uid: String) {
        if (_state.value is SyncState.Loading) return

        viewModelScope.launch {
            _state.value = SyncState.Loading

            try {
                repository.restore(uid)

                _state.value = SyncState.Success(
                    "Cloud backup restored successfully!"
                )
            } catch (e: Exception) {
                _state.value = SyncState.Error(
                    e.localizedMessage ?: "Restore failed."
                )
            }
        }
    }
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Loading : SyncState
    data class Success(val message: String) : SyncState
    data class Error(val message: String) : SyncState
}
