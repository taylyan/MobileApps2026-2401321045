
package com.huntersstory.app.ui.collection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huntersstory.app.data.local.HunterDatabase
import com.huntersstory.app.data.repository.CollectionItem
import com.huntersstory.app.data.repository.CollectionRepository
import com.huntersstory.app.data.repository.CollectionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CollectionViewModel
    @JvmOverloads
    constructor(
        application: Application,
        private val repository: CollectionRepository =
            CollectionRepository(
                HunterDatabase.getDatabase(application),
            ),
    ) : AndroidViewModel(application) {
        fun observe(
            type: CollectionType,
            uid: String,
        ): Flow<List<CollectionItem>> = repository.observe(type, uid)

        // Room and validation errors become UI messages.
        // Coroutine cancellation is always propagated.
        @Suppress("TooGenericExceptionCaught")
        fun save(
            type: CollectionType,
            uid: String,
            item: CollectionItem,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
        ) {
            viewModelScope.launch {
                try {
                    repository.save(type, uid, item)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onError(
                        e.localizedMessage
                            ?: "Failed to save record.",
                    )
                    return@launch
                }

                onSuccess()
            }
        }

        // Room and validation errors become UI messages.
        // Coroutine cancellation is always propagated.
        @Suppress("TooGenericExceptionCaught")
        fun delete(
            type: CollectionType,
            uid: String,
            id: Long,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
        ) {
            viewModelScope.launch {
                try {
                    repository.delete(type, uid, id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onError(
                        e.localizedMessage
                            ?: "Failed to delete record.",
                    )
                    return@launch
                }

                onSuccess()
            }
        }
    }
