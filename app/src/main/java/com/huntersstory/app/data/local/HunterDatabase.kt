
package com.huntersstory.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.huntersstory.app.data.local.dao.*
import com.huntersstory.app.data.local.entity.*

@Database(
    entities = [
        HuntingTrip::class,
        HuntingSpot::class,
        Equipment::class,
        HuntingDog::class,
        HuntingPhoto::class
    ],
    version = 4,
    exportSchema = false
)
abstract class HunterDatabase : RoomDatabase() {

    abstract fun huntingTripDao(): HuntingTripDao
    abstract fun huntingSpotDao(): HuntingSpotDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun huntingDogDao(): HuntingDogDao
    abstract fun huntingPhotoDao(): HuntingPhotoDao

    companion object {

        @Volatile
        private var INSTANCE: HunterDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS hunting_spots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        notes TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS equipment (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        notes TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS hunting_dogs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        breed TEXT NOT NULL,
                        ageYears INTEGER NOT NULL,
                        notes TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS hunting_photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): HunterDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HunterDatabase::class.java,
                    "hunter_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
