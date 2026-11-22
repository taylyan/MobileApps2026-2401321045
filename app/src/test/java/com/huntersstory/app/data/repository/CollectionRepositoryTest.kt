package com.huntersstory.app.data.repository

import com.huntersstory.app.data.local.HunterDatabase
import com.huntersstory.app.data.local.dao.EquipmentDao
import com.huntersstory.app.data.local.dao.HuntingDogDao
import com.huntersstory.app.data.local.entity.Equipment
import com.huntersstory.app.data.local.entity.HuntingDog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CollectionRepositoryTest {

    private lateinit var db: HunterDatabase
    private lateinit var equipmentDao: EquipmentDao
    private lateinit var dogDao: HuntingDogDao
    private lateinit var repository: CollectionRepository

    private val uid = "test-user"

    @Before
    fun setup() {
        db = mockk()
        equipmentDao = mockk()
        dogDao = mockk()

        every { db.equipmentDao() } returns equipmentDao
        every { db.huntingDogDao() } returns dogDao

        repository = CollectionRepository(db)
    }

    @Test
    fun observeEquipmentReturnsItems() = runBlocking {
        every { equipmentDao.getByUser(uid) } returns flowOf(
            listOf(
                Equipment(
                    id = 10,
                    userId = uid,
                    name = "Binoculars",
                    category = "Optics",
                    notes = "10x42"
                )
            )
        )

        val result = repository.observe(
            CollectionType.EQUIPMENT, uid
        ).first()

        assertEquals(1, result.size)
        assertEquals(10L, result[0].id)
        assertEquals("Binoculars", result[0].name)
        assertEquals("Optics", result[0].secondary)
        assertEquals("10x42", result[0].notes)
        assertNull(result[0].ageYears)
    }

    @Test
    fun observeEquipmentCanReturnEmptyList() = runBlocking {
        every { equipmentDao.getByUser(uid) } returns
                flowOf(emptyList())

        val result = repository.observe(
            CollectionType.EQUIPMENT, uid
        ).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun observeDogsReturnsItems() = runBlocking {
        every { dogDao.getByUser(uid) } returns flowOf(
            listOf(
                HuntingDog(
                    id = 20,
                    userId = uid,
                    name = "Rex",
                    breed = "Pointer",
                    ageYears = 3,
                    notes = "Trained"
                )
            )
        )

        val result = repository.observe(
            CollectionType.DOGS, uid
        ).first()

        assertEquals(1, result.size)
        assertEquals(20L, result[0].id)
        assertEquals("Rex", result[0].name)
        assertEquals("Pointer", result[0].secondary)
        assertEquals(3, result[0].ageYears)
        assertEquals("Trained", result[0].notes)
    }

    @Test
    fun observeDogsCanReturnEmptyList() = runBlocking {
        every { dogDao.getByUser(uid) } returns
                flowOf(emptyList())

        val result = repository.observe(
            CollectionType.DOGS, uid
        ).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun insertEquipmentUsesDao() = runBlocking {
        coEvery {
            equipmentDao.insert(any())
        } returns 1L

        repository.save(
            CollectionType.EQUIPMENT,
            uid,
            CollectionItem(
                name = "Backpack",
                secondary = "Accessories",
                notes = "30L"
            )
        )

        coVerify(exactly = 1) {
            equipmentDao.insert(
                match {
                    it.userId == uid &&
                            it.name == "Backpack" &&
                            it.category == "Accessories" &&
                            it.notes == "30L"
                }
            )
        }
    }

    @Test
    fun updateEquipmentUsesDao() = runBlocking {
        coEvery {
            equipmentDao.update(
                5L, uid, "Scope", "Optics", "Updated"
            )
        } returns 1

        repository.save(
            CollectionType.EQUIPMENT,
            uid,
            CollectionItem(
                id = 5,
                name = "Scope",
                secondary = "Optics",
                notes = "Updated"
            )
        )

        coVerify(exactly = 1) {
            equipmentDao.update(
                5L, uid, "Scope", "Optics", "Updated"
            )
        }
    }

    @Test
    fun updateMissingEquipmentThrows() = runBlocking {
        coEvery {
            equipmentDao.update(any(), any(), any(), any(), any())
        } returns 0

        val error = runCatching {
            repository.save(
                CollectionType.EQUIPMENT,
                uid,
                CollectionItem(
                    id = 99,
                    name = "Missing",
                    secondary = "Other"
                )
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }

    @Test
    fun insertDogUsesDao() = runBlocking {
        coEvery { dogDao.insert(any()) } returns 2L

        repository.save(
            CollectionType.DOGS,
            uid,
            CollectionItem(
                name = "Rex",
                secondary = "Pointer",
                ageYears = 3,
                notes = "Trained"
            )
        )

        coVerify(exactly = 1) {
            dogDao.insert(
                match {
                    it.userId == uid &&
                            it.name == "Rex" &&
                            it.breed == "Pointer" &&
                            it.ageYears == 3 &&
                            it.notes == "Trained"
                }
            )
        }
    }

    @Test
    fun updateDogUsesDao() = runBlocking {
        coEvery {
            dogDao.update(
                7L, uid, "Max", "Setter", 4, "Updated"
            )
        } returns 1

        repository.save(
            CollectionType.DOGS,
            uid,
            CollectionItem(
                id = 7,
                name = "Max",
                secondary = "Setter",
                ageYears = 4,
                notes = "Updated"
            )
        )

        coVerify(exactly = 1) {
            dogDao.update(
                7L, uid, "Max", "Setter", 4, "Updated"
            )
        }
    }

    @Test
    fun updateMissingDogThrows() = runBlocking {
        coEvery {
            dogDao.update(
                any(), any(), any(), any(), any(), any()
            )
        } returns 0

        val error = runCatching {
            repository.save(
                CollectionType.DOGS,
                uid,
                CollectionItem(
                    id = 99,
                    name = "Missing",
                    secondary = "Pointer",
                    ageYears = 2
                )
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }

    @Test
    fun dogWithoutAgeIsRejected() = runBlocking {
        val error = runCatching {
            repository.save(
                CollectionType.DOGS,
                uid,
                CollectionItem(
                    name = "Rex",
                    secondary = "Pointer",
                    ageYears = null
                )
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun deleteEquipmentUsesCorrectUser() = runBlocking {
        coEvery {
            equipmentDao.delete(5L, uid)
        } returns 1

        repository.delete(
            CollectionType.EQUIPMENT, uid, 5L
        )

        coVerify(exactly = 1) {
            equipmentDao.delete(5L, uid)
        }
    }

    @Test
    fun deleteDogUsesCorrectUser() = runBlocking {
        coEvery {
            dogDao.delete(7L, uid)
        } returns 1

        repository.delete(
            CollectionType.DOGS, uid, 7L
        )

        coVerify(exactly = 1) {
            dogDao.delete(7L, uid)
        }
    }

    @Test
    fun deletingMissingEquipmentThrows() = runBlocking {
        coEvery {
            equipmentDao.delete(99L, uid)
        } returns 0

        val error = runCatching {
            repository.delete(
                CollectionType.EQUIPMENT, uid, 99L
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }

    @Test
    fun deletingMissingDogThrows() = runBlocking {
        coEvery {
            dogDao.delete(99L, uid)
        } returns 0

        val error = runCatching {
            repository.delete(
                CollectionType.DOGS, uid, 99L
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }
}
