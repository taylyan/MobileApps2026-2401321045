
package com.huntersstory.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hunting_trips")
data class HuntingTrip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,
    val date: String,
    val location: String,
    val gameSpecies: String,
    val notes: String = ""
)
