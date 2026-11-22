
package com.huntersstory.app.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huntersstory.app.data.local.entity.HuntingPhoto
import com.huntersstory.app.data.repository.PhotoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryViewModel
    @JvmOverloads
    constructor(
        application: Application,
        private val repository: PhotoRepository =
            PhotoRepository(application),
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : AndroidViewModel(application) {
        fun observe(uid: String): Flow<List<HuntingPhoto>> = repository.observe(uid)

        fun photoFile(photo: HuntingPhoto): File = repository.getFile(photo)

        // Repository errors are converted into UI messages.
        // Coroutine cancellation must still propagate.
        @Suppress("TooGenericExceptionCaught")
        fun save(
            uid: String,
            fileName: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
        ) {
            viewModelScope.launch {
                try {
                    withContext(ioDispatcher) {
                        repository.save(uid, fileName)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Save failed.")
                    return@launch
                }

                onSuccess()
            }
        }

        // Repository errors are converted into UI messages.
        // Coroutine cancellation must still propagate.
        @Suppress("TooGenericExceptionCaught")
        fun delete(
            photo: HuntingPhoto,
            uid: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
        ) {
            viewModelScope.launch {
                try {
                    withContext(ioDispatcher) {
                        repository.delete(photo, uid)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Delete failed.")
                    return@launch
                }

                onSuccess()
            }
        }
    }
