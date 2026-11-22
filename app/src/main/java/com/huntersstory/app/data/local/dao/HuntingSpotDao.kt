
package com.huntersstory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.huntersstory.app.data.local.entity.HuntingSpot
import kotlinx.coroutines.flow.Flow

@Dao
interface HuntingSpotDao {

    @Insert
    suspend fun insert(spot: HuntingSpot): Long

    @Query(
        "SELECT * FROM hunting_spots WHERE userId = :userId ORDER BY id DESC"
    )
    fun getSpotsByUser(userId: String): Flow<List<HuntingSpot>>

    @Query(
        "DELETE FROM hunting_spots WHERE id = :id AND userId = :userId"
    )
    suspend fun deleteById(id: Long, userId: String): Int

    @Query("DELETE FROM hunting_spots WHERE userId = :uid")
    suspend fun deleteAllForUser(uid: String)

}
