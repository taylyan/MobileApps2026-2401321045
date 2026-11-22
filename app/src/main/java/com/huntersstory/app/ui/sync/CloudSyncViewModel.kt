
package com.huntersstory.app.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huntersstory.app.data.repository.CloudBackupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CloudSyncViewModel
    @JvmOverloads
    constructor(
        application: Application,
        private val repository: CloudBackupRepository =
            CloudBackupRepository(application),
    ) : AndroidViewModel(application) {
        private val _state =
            MutableStateFlow<SyncState>(
                SyncState.Idle,
            )

        val state = _state.asStateFlow()

        // Firebase and backup errors become UI states.
        // Coroutine cancellation must propagate.
        @Suppress("TooGenericExceptionCaught")
        fun upload(uid: String) {
            if (_state.value is SyncState.Loading) {
                return
            }

            viewModelScope.launch {
                _state.value = SyncState.Loading

                try {
                    repository.upload(uid)
                } catch (e: CancellationException) {
                    _state.value = SyncState.Idle
                    throw e
                } catch (e: Exception) {
                    _state.value =
                        SyncState.Error(
                            e.localizedMessage
                                ?: "Upload failed.",
                        )
                    return@launch
                }

                _state.value =
                    SyncState.Success(
                        "Cloud backup uploaded successfully!",
                    )
            }
        }

        // Firebase and backup errors become UI states.
        // Coroutine cancellation must propagate.
        @Suppress("TooGenericExceptionCaught")
        fun restore(uid: String) {
            if (_state.value is SyncState.Loading) {
                return
            }

            viewModelScope.launch {
                _state.value = SyncState.Loading

                try {
                    repository.restore(uid)
                } catch (e: CancellationException) {
                    _state.value = SyncState.Idle
                    throw e
                } catch (e: Exception) {
                    _state.value =
                        SyncState.Error(
                            e.localizedMessage
                                ?: "Restore failed.",
                        )
                    return@launch
                }

                _state.value =
                    SyncState.Success(
                        "Cloud backup restored successfully!",
                    )
            }
        }
    }

sealed interface SyncState {
    data object Idle : SyncState

    data object Loading : SyncState

    data class Success(
        val message: String,
    ) : SyncState

    data class Error(
        val message: String,
    ) : SyncState
}
