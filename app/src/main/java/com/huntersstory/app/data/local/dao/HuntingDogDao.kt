package com.huntersstory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.huntersstory.app.data.local.entity.HuntingDog
import kotlinx.coroutines.flow.Flow

@Dao
interface HuntingDogDao {

    @Query("SELECT * FROM hunting_dogs WHERE userId = :uid ORDER BY id DESC")
    fun getByUser(uid: String): Flow<List<HuntingDog>>

    @Insert
    suspend fun insert(dog: HuntingDog): Long

    @Query("""
        UPDATE hunting_dogs
        SET name = :name, breed = :breed,
            ageYears = :age, notes = :notes
        WHERE id = :id AND userId = :uid
    """)
    suspend fun update(
        id: Long,
        uid: String,
        name: String,
        breed: String,
        age: Int,
        notes: String
    ): Int

    @Query("DELETE FROM hunting_dogs WHERE id = :id AND userId = :uid")
    suspend fun delete(id: Long, uid: String): Int
    @Query("DELETE FROM hunting_dogs WHERE userId = :uid")
    suspend fun deleteAllForUser(uid: String)

}
