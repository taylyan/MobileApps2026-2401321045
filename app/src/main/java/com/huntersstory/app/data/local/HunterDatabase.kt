
package com.huntersstory.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.huntersstory.app.data.local.dao.HuntingTripDao
import com.huntersstory.app.data.local.dao.HuntingSpotDao
import com.huntersstory.app.data.local.entity.HuntingTrip
import com.huntersstory.app.data.local.entity.HuntingSpot

@Database(
    entities = [HuntingTrip::class, HuntingSpot::class],
    version = 2,
    exportSchema = false
)
abstract class HunterDatabase : RoomDatabase() {

    abstract fun huntingTripDao(): HuntingTripDao
    abstract fun huntingSpotDao(): HuntingSpotDao

    companion object {

        @Volatile
        private var INSTANCE: HunterDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS hunting_spots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): HunterDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HunterDatabase::class.java,
                    "hunter_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
