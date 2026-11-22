package com.huntersstory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.huntersstory.app.data.local.entity.Equipment
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {

    @Query("SELECT * FROM equipment WHERE userId = :uid ORDER BY id DESC")
    fun getByUser(uid: String): Flow<List<Equipment>>

    @Insert
    suspend fun insert(item: Equipment): Long

    @Query("""
        UPDATE equipment
        SET name = :name, category = :category, notes = :notes
        WHERE id = :id AND userId = :uid
    """)
    suspend fun update(
        id: Long,
        uid: String,
        name: String,
        category: String,
        notes: String
    ): Int

    @Query("DELETE FROM equipment WHERE id = :id AND userId = :uid")
    suspend fun delete(id: Long, uid: String): Int

    @Query("DELETE FROM equipment WHERE userId = :uid")
    suspend fun deleteAllForUser(uid: String)

}
