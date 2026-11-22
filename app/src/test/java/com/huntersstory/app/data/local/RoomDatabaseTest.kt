package com.huntersstory.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.huntersstory.app.data.local.entity.Equipment
import com.huntersstory.app.data.local.entity.HuntingDog
import com.huntersstory.app.data.local.entity.HuntingPhoto
import com.huntersstory.app.data.local.entity.HuntingSpot
import com.huntersstory.app.data.local.entity.HuntingTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomDatabaseTest {
    private lateinit var db: HunterDatabase

    private val uid = "hunter-test-1"
    private val otherUid = "hunter-test-2"

    @Before
    fun setup() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        db =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    HunterDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
    }

    @After
    fun cleanup() {
        db.close()
    }

    private fun trip(
        user: String = uid,
        location: String = "Kuklen",
    ) = HuntingTrip(
        userId = user,
        date = "2026-11-22",
        location = location,
        gameSpecies = "Pheasant",
        notes = "Morning trip",
    )

    private fun spot(
        user: String = uid,
        name: String = "Forest Spot",
    ) = HuntingSpot(
        userId = user,
        name = name,
        latitude = 42.035,
        longitude = 24.786,
        notes = "Near the trees",
    )

    private fun equipment(
        user: String = uid,
        name: String = "Binoculars",
    ) = Equipment(
        userId = user,
        name = name,
        category = "Optics",
        notes = "10x42",
    )

    private fun dog(
        user: String = uid,
        name: String = "Rex",
    ) = HuntingDog(
        userId = user,
        name = name,
        breed = "Pointer",
        ageYears = 3,
        notes = "Trained",
    )

    // ========================
    // HUNTING JOURNAL
    // ========================

    @Test
    fun insertAndReadTrip() =
        runBlocking {
            val id = db.huntingTripDao().insert(trip())

            val result =
                db
                    .huntingTripDao()
                    .getTripsByUser(uid)
                    .first()

            assertEquals(1, result.size)
            assertEquals(id, result[0].id)
            assertEquals("Kuklen", result[0].location)
        }

    @Test
    fun getTripById() =
        runBlocking {
            val id = db.huntingTripDao().insert(trip())

            val result =
                db
                    .huntingTripDao()
                    .getTripById(id, uid)

            assertNotNull(result)
            assertEquals("Pheasant", result?.gameSpecies)
        }

    @Test
    fun updateTrip() =
        runBlocking {
            val dao = db.huntingTripDao()
            val id = dao.insert(trip())

            val original = dao.getTripById(id, uid)!!
            dao.update(original.copy(notes = "Updated"))

            val updated = dao.getTripById(id, uid)

            assertEquals("Updated", updated?.notes)
        }

    @Test
    fun deleteTrip() =
        runBlocking {
            val dao = db.huntingTripDao()
            val id = dao.insert(trip())

            val saved = dao.getTripById(id, uid)!!
            dao.delete(saved)

            assertTrue(dao.getTripsByUser(uid).first().isEmpty())
        }

    @Test
    fun tripsAreFilteredByUser() =
        runBlocking {
            val dao = db.huntingTripDao()

            dao.insert(trip(uid))
            dao.insert(trip(otherUid))

            assertEquals(1, dao.getTripsByUser(uid).first().size)
            assertEquals(
                1,
                dao.getTripsByUser(otherUid).first().size,
            )
        }

    @Test
    fun getTripWithWrongUserReturnsNull() =
        runBlocking {
            val dao = db.huntingTripDao()
            val id = dao.insert(trip(uid))

            assertNull(dao.getTripById(id, otherUid))
        }

    @Test
    fun deleteAllTripsForOneUser() =
        runBlocking {
            val dao = db.huntingTripDao()

            dao.insert(trip(uid))
            dao.insert(trip(uid))
            dao.insert(trip(otherUid))

            dao.deleteAllForUser(uid)

            assertTrue(dao.getTripsByUser(uid).first().isEmpty())
            assertEquals(
                1,
                dao.getTripsByUser(otherUid).first().size,
            )
        }

    @Test
    fun tripsAreOrderedByDateDescending() =
        runBlocking {
            val dao = db.huntingTripDao()

            dao.insert(trip().copy(date = "2026-01-01"))
            dao.insert(trip().copy(date = "2026-12-31"))

            val result = dao.getTripsByUser(uid).first()

            assertEquals("2026-12-31", result.first().date)
        }

    // ========================
    // HUNTING SPOTS
    // ========================

    @Test
    fun insertAndReadSpot() =
        runBlocking {
            val dao = db.huntingSpotDao()

            val id = dao.insert(spot())
            val result = dao.getSpotsByUser(uid).first()

            assertEquals(1, result.size)
            assertEquals(id, result[0].id)
            assertEquals(42.035, result[0].latitude, 0.000001)
            assertEquals(24.786, result[0].longitude, 0.000001)
        }

    @Test
    fun spotsAreFilteredByUser() =
        runBlocking {
            val dao = db.huntingSpotDao()

            dao.insert(spot(uid))
            dao.insert(spot(otherUid))

            assertEquals(1, dao.getSpotsByUser(uid).first().size)
        }

    @Test
    fun deleteSpotById() =
        runBlocking {
            val dao = db.huntingSpotDao()

            val id = dao.insert(spot())

            assertEquals(1, dao.deleteById(id, uid))
            assertTrue(dao.getSpotsByUser(uid).first().isEmpty())
        }

    @Test
    fun cannotDeleteAnotherUsersSpot() =
        runBlocking {
            val dao = db.huntingSpotDao()

            val id = dao.insert(spot(uid))

            assertEquals(0, dao.deleteById(id, otherUid))
            assertEquals(1, dao.getSpotsByUser(uid).first().size)
        }

    @Test
    fun deleteAllSpotsForOneUser() =
        runBlocking {
            val dao = db.huntingSpotDao()

            dao.insert(spot(uid))
            dao.insert(spot(uid))
            dao.insert(spot(otherUid))

            dao.deleteAllForUser(uid)

            assertTrue(dao.getSpotsByUser(uid).first().isEmpty())
            assertEquals(
                1,
                dao.getSpotsByUser(otherUid).first().size,
            )
        }

    // ========================
    // MY EQUIPMENT
    // ========================

    @Test
    fun insertAndReadEquipment() =
        runBlocking {
            val dao = db.equipmentDao()

            val id = dao.insert(equipment())
            val result = dao.getByUser(uid).first()

            assertEquals(1, result.size)
            assertEquals(id, result[0].id)
            assertEquals("Binoculars", result[0].name)
        }

    @Test
    fun updateEquipment() =
        runBlocking {
            val dao = db.equipmentDao()
            val id = dao.insert(equipment())

            val changed =
                dao.update(
                    id,
                    uid,
                    "Scope",
                    "Optics",
                    "Updated",
                )

            val result = dao.getByUser(uid).first()

            assertEquals(1, changed)
            assertEquals("Scope", result[0].name)
            assertEquals("Updated", result[0].notes)
        }

    @Test
    fun cannotUpdateAnotherUsersEquipment() =
        runBlocking {
            val dao = db.equipmentDao()
            val id = dao.insert(equipment(uid))

            val changed =
                dao.update(
                    id,
                    otherUid,
                    "Changed",
                    "Other",
                    "",
                )

            assertEquals(0, changed)
            assertEquals(
                "Binoculars",
                dao.getByUser(uid).first()[0].name,
            )
        }

    @Test
    fun deleteEquipment() =
        runBlocking {
            val dao = db.equipmentDao()
            val id = dao.insert(equipment())

            assertEquals(1, dao.delete(id, uid))
            assertTrue(dao.getByUser(uid).first().isEmpty())
        }

    @Test
    fun cannotDeleteAnotherUsersEquipment() =
        runBlocking {
            val dao = db.equipmentDao()
            val id = dao.insert(equipment(uid))

            assertEquals(0, dao.delete(id, otherUid))
            assertEquals(1, dao.getByUser(uid).first().size)
        }

    @Test
    fun deleteAllEquipmentForOneUser() =
        runBlocking {
            val dao = db.equipmentDao()

            dao.insert(equipment(uid))
            dao.insert(equipment(uid))
            dao.insert(equipment(otherUid))

            dao.deleteAllForUser(uid)

            assertTrue(dao.getByUser(uid).first().isEmpty())
            assertEquals(
                1,
                dao.getByUser(otherUid).first().size,
            )
        }

    // ========================
    // MY DOGS
    // ========================

    @Test
    fun insertAndReadDog() =
        runBlocking {
            val dao = db.huntingDogDao()

            val id = dao.insert(dog())
            val result = dao.getByUser(uid).first()

            assertEquals(1, result.size)
            assertEquals(id, result[0].id)
            assertEquals("Rex", result[0].name)
            assertEquals(3, result[0].ageYears)
        }

    @Test
    fun updateDog() =
        runBlocking {
            val dao = db.huntingDogDao()
            val id = dao.insert(dog())

            val changed =
                dao.update(
                    id,
                    uid,
                    "Max",
                    "Setter",
                    4,
                    "Updated",
                )

            val result = dao.getByUser(uid).first()

            assertEquals(1, changed)
            assertEquals("Max", result[0].name)
            assertEquals(4, result[0].ageYears)
        }

    @Test
    fun cannotUpdateAnotherUsersDog() =
        runBlocking {
            val dao = db.huntingDogDao()
            val id = dao.insert(dog(uid))

            assertEquals(
                0,
                dao.update(
                    id,
                    otherUid,
                    "Changed",
                    "Other",
                    5,
                    "",
                ),
            )

            assertEquals(
                "Rex",
                dao.getByUser(uid).first()[0].name,
            )
        }

    @Test
    fun deleteDog() =
        runBlocking {
            val dao = db.huntingDogDao()
            val id = dao.insert(dog())

            assertEquals(1, dao.delete(id, uid))
            assertTrue(dao.getByUser(uid).first().isEmpty())
        }

    @Test
    fun cannotDeleteAnotherUsersDog() =
        runBlocking {
            val dao = db.huntingDogDao()
            val id = dao.insert(dog(uid))

            assertEquals(0, dao.delete(id, otherUid))
            assertEquals(1, dao.getByUser(uid).first().size)
        }

    @Test
    fun deleteAllDogsForOneUser() =
        runBlocking {
            val dao = db.huntingDogDao()

            dao.insert(dog(uid))
            dao.insert(dog(uid))
            dao.insert(dog(otherUid))

            dao.deleteAllForUser(uid)

            assertTrue(dao.getByUser(uid).first().isEmpty())
            assertEquals(
                1,
                dao.getByUser(otherUid).first().size,
            )
        }

    // ========================
    // HUNTING GALLERY
    // ========================

    @Test
    fun insertAndReadPhoto() =
        runBlocking {
            val dao = db.huntingPhotoDao()

            val id =
                dao.insert(
                    HuntingPhoto(
                        userId = uid,
                        fileName = "photo1.jpg",
                        createdAt = 1000L,
                    ),
                )

            val result = dao.getByUser(uid).first()

            assertEquals(1, result.size)
            assertEquals(id, result[0].id)
            assertEquals("photo1.jpg", result[0].fileName)
        }

    @Test
    fun photosAreFilteredByUser() =
        runBlocking {
            val dao = db.huntingPhotoDao()

            dao.insert(
                HuntingPhoto(
                    userId = uid,
                    fileName = "mine.jpg",
                    createdAt = 1000L,
                ),
            )

            dao.insert(
                HuntingPhoto(
                    userId = otherUid,
                    fileName = "other.jpg",
                    createdAt = 2000L,
                ),
            )

            val result = dao.getByUser(uid).first()

            assertEquals(1, result.size)
            assertEquals("mine.jpg", result[0].fileName)
        }

    @Test
    fun photosAreSortedNewestFirst() =
        runBlocking {
            val dao = db.huntingPhotoDao()

            dao.insert(
                HuntingPhoto(
                    userId = uid,
                    fileName = "old.jpg",
                    createdAt = 100L,
                ),
            )

            dao.insert(
                HuntingPhoto(
                    userId = uid,
                    fileName = "new.jpg",
                    createdAt = 200L,
                ),
            )

            val result = dao.getByUser(uid).first()

            assertEquals("new.jpg", result[0].fileName)
        }

    @Test
    fun deletePhoto() =
        runBlocking {
            val dao = db.huntingPhotoDao()

            val id =
                dao.insert(
                    HuntingPhoto(
                        userId = uid,
                        fileName = "delete.jpg",
                        createdAt = 100L,
                    ),
                )

            assertEquals(1, dao.deleteById(id, uid))
            assertTrue(dao.getByUser(uid).first().isEmpty())
        }

    @Test
    fun cannotDeleteAnotherUsersPhoto() =
        runBlocking {
            val dao = db.huntingPhotoDao()

            val id =
                dao.insert(
                    HuntingPhoto(
                        userId = uid,
                        fileName = "private.jpg",
                        createdAt = 100L,
                    ),
                )

            assertEquals(0, dao.deleteById(id, otherUid))
            assertEquals(1, dao.getByUser(uid).first().size)
        }
}
