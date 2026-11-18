package com.huntersstory.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.huntersstory.app.data.local.dao.HuntingTripDao
import com.huntersstory.app.data.local.entity.HuntingTrip

@Database(
    entities = [HuntingTrip::class],
    version = 1,
    exportSchema = false
)
abstract class HunterDatabase : RoomDatabase() {

    abstract fun huntingTripDao(): HuntingTripDao

    companion object {

        @Volatile
        private var INSTANCE: HunterDatabase? = null

        fun getDatabase(context: Context): HunterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HunterDatabase::class.java,
                    "hunter_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
