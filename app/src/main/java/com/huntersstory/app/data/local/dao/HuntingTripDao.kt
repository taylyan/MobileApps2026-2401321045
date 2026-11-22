package com.huntersstory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.huntersstory.app.data.local.entity.HuntingTrip
import kotlinx.coroutines.flow.Flow

@Dao
interface HuntingTripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: HuntingTrip): Long

    @Update
    suspend fun update(trip: HuntingTrip)

    @Delete
    suspend fun delete(trip: HuntingTrip)

    @Query("""
        SELECT * FROM hunting_trips
        WHERE userId = :userId
        ORDER BY date DESC
    """)
    fun getTripsByUser(userId: String): Flow<List<HuntingTrip>>

    @Query("""
        SELECT * FROM hunting_trips
        WHERE id = :tripId AND userId = :userId
        LIMIT 1
    """)
    suspend fun getTripById(
        tripId: Long,
        userId: String
    ): HuntingTrip?

    @Query("DELETE FROM hunting_trips WHERE userId = :uid")
    suspend fun deleteAllForUser(uid: String)
}
