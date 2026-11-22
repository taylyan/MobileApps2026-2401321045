
package com.huntersstory.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hunting_spots")
data class HuntingSpot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
)
