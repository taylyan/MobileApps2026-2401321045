
package com.huntersstory.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hunting_photos")
data class HuntingPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val fileName: String,
    val createdAt: Long
)
