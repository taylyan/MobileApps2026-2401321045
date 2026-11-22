
package com.huntersstory.app.data.repository

import android.content.Context
import com.huntersstory.app.data.local.HunterDatabase
import com.huntersstory.app.data.local.dao.HuntingPhotoDao
import com.huntersstory.app.data.local.entity.HuntingPhoto
import kotlinx.coroutines.flow.Flow
import java.io.File

class PhotoRepository(
    context: Context,
    private val dao: HuntingPhotoDao =
        HunterDatabase
            .getDatabase(
                context.applicationContext,
            ).huntingPhotoDao(),
) {
    private val appContext = context.applicationContext

    val photoDirectory: File
        get() =
            File(appContext.filesDir, "photos")
                .apply { mkdirs() }

    fun observe(uid: String): Flow<List<HuntingPhoto>> = dao.getByUser(uid)

    fun getFile(photo: HuntingPhoto): File = File(photoDirectory, photo.fileName)

    suspend fun save(
        uid: String,
        fileName: String,
    ) {
        val file = File(photoDirectory, fileName)

        require(file.isFile && file.length() > 0) {
            "Photo file is missing or empty."
        }

        dao.insert(
            HuntingPhoto(
                userId = uid,
                fileName = fileName,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(
        photo: HuntingPhoto,
        uid: String,
    ) {
        val deleted = dao.deleteById(photo.id, uid)

        check(deleted > 0) {
            "Photo not found."
        }

        val file = getFile(photo)
        if (file.exists() && !file.delete()) {
            // Database record is removed; file cleanup can be retried.
            android.util.Log.w(
                "PhotoRepository",
                "Could not delete photo file: ${file.name}",
            )
        }
    }
}
