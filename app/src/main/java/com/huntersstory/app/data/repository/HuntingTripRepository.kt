
package com.huntersstory.app.data.repository

import com.huntersstory.app.data.local.dao.HuntingTripDao
import com.huntersstory.app.data.local.entity.HuntingTrip
import kotlinx.coroutines.flow.Flow

class HuntingTripRepository(
    private val dao: HuntingTripDao,
) {
    fun getTripsByUser(userId: String): Flow<List<HuntingTrip>> = dao.getTripsByUser(userId)

    suspend fun getTripById(
        tripId: Long,
        userId: String,
    ): HuntingTrip? = dao.getTripById(tripId, userId)

    suspend fun insertTrip(trip: HuntingTrip): Long = dao.insert(trip)

    suspend fun updateTrip(trip: HuntingTrip) {
        dao.update(trip)
    }

    suspend fun deleteTrip(trip: HuntingTrip) {
        dao.delete(trip)
    }
}
