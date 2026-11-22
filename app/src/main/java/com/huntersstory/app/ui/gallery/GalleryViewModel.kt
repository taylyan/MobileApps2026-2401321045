
package com.huntersstory.app.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huntersstory.app.data.local.entity.HuntingPhoto
import com.huntersstory.app.data.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = PhotoRepository(application)

    fun observe(uid: String): Flow<List<HuntingPhoto>> {
        return repository.observe(uid)
    }

    fun photoFile(photo: HuntingPhoto): File {
        return repository.getFile(photo)
    }

    fun save(
        uid: String,
        fileName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.save(uid, fileName)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Save failed.")
            }
        }
    }

    fun delete(
        photo: HuntingPhoto,
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.delete(photo, uid)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Delete failed.")
            }
        }
    }
}
