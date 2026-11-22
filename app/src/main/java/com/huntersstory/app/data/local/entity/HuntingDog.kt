
package com.huntersstory.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hunting_dogs")
data class HuntingDog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val breed: String,
    val ageYears: Int,
    val notes: String = "",
)
