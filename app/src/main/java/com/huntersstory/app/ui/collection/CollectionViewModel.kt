
package com.huntersstory.app.ui.collection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huntersstory.app.data.local.HunterDatabase
import com.huntersstory.app.data.repository.CollectionItem
import com.huntersstory.app.data.repository.CollectionRepository
import com.huntersstory.app.data.repository.CollectionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CollectionViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: CollectionRepository =
        CollectionRepository(
            HunterDatabase.getDatabase(application)
        )
) : AndroidViewModel(application) {

    fun observe(
        type: CollectionType,
        uid: String
    ): Flow<List<CollectionItem>> {
        return repository.observe(type, uid)
    }

    fun save(
        type: CollectionType,
        uid: String,
        item: CollectionItem,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.save(type, uid, item)
                onSuccess()
            } catch (e: Exception) {
                onError(
                    e.localizedMessage ?: "Failed to save record."
                )
            }
        }
    }

    fun delete(
        type: CollectionType,
        uid: String,
        id: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.delete(type, uid, id)
                onSuccess()
            } catch (e: Exception) {
                onError(
                    e.localizedMessage ?: "Failed to delete record."
                )
            }
        }
    }
}
