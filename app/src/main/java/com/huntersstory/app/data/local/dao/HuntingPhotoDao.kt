package com.huntersstory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.huntersstory.app.data.local.entity.HuntingPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface HuntingPhotoDao {
    @Query(
        """
        SELECT * FROM hunting_photos
        WHERE userId = :uid
        ORDER BY createdAt DESC
    """,
    )
    fun getByUser(uid: String): Flow<List<HuntingPhoto>>

    @Insert
    suspend fun insert(photo: HuntingPhoto): Long

    @Query(
        """
        DELETE FROM hunting_photos
        WHERE id = :id AND userId = :uid
    """,
    )
    suspend fun deleteById(
        id: Long,
        uid: String,
    ): Int
}
