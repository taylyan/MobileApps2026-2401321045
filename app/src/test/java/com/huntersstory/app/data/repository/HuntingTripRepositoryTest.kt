package com.huntersstory.app.data.repository

import com.huntersstory.app.data.local.dao.HuntingTripDao
import com.huntersstory.app.data.local.entity.HuntingTrip
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HuntingTripRepositoryTest {
    private lateinit var dao: HuntingTripDao
    private lateinit var repository: HuntingTripRepository

    private val uid = "test-user"

    private val trip =
        HuntingTrip(
            id = 10L,
            userId = uid,
            date = "2026-11-22",
            location = "Kuklen",
            gameSpecies = "Pheasant",
            notes = "Morning hunting trip",
        )

    @Before
    fun setup() {
        dao = mockk()
        repository = HuntingTripRepository(dao)
    }

    @Test
    fun getTripsReturnsSavedRecords() =
        runBlocking {
            every { dao.getTripsByUser(uid) } returns
                flowOf(listOf(trip))

            val result =
                repository
                    .getTripsByUser(uid)
                    .first()

            assertEquals(1, result.size)
            assertEquals("Kuklen", result[0].location)
            assertEquals("Pheasant", result[0].gameSpecies)
        }

    @Test
    fun getTripsReturnsEmptyList() =
        runBlocking {
            every { dao.getTripsByUser(uid) } returns
                flowOf(emptyList())

            val result =
                repository
                    .getTripsByUser(uid)
                    .first()

            assertTrue(result.isEmpty())
        }

    @Test
    fun getTripByIdReturnsCorrectRecord() =
        runBlocking {
            coEvery {
                dao.getTripById(10L, uid)
            } returns trip

            val result = repository.getTripById(10L, uid)

            assertNotNull(result)
            assertEquals(10L, result?.id)
            assertEquals(uid, result?.userId)
        }

    @Test
    fun getMissingTripReturnsNull() =
        runBlocking {
            coEvery {
                dao.getTripById(99L, uid)
            } returns null

            val result = repository.getTripById(99L, uid)

            assertNull(result)
        }

    @Test
    fun insertTripCallsDao() =
        runBlocking {
            coEvery { dao.insert(trip) } returns 10L

            val result = repository.insertTrip(trip)

            assertEquals(10L, result)

            coVerify(exactly = 1) {
                dao.insert(trip)
            }
        }

    @Test
    fun updateTripCallsDao() =
        runBlocking {
            val updated =
                trip.copy(
                    notes = "Updated notes",
                )

            coEvery { dao.update(updated) } returns Unit

            repository.updateTrip(updated)

            coVerify(exactly = 1) {
                dao.update(updated)
            }
        }

    @Test
    fun deleteTripCallsDao() =
        runBlocking {
            coEvery { dao.delete(trip) } returns Unit

            repository.deleteTrip(trip)

            coVerify(exactly = 1) {
                dao.delete(trip)
            }
        }

    @Test
    fun insertFailureIsPropagated() =
        runBlocking {
            coEvery {
                dao.insert(trip)
            } throws IllegalStateException("Database error")

            val error =
                runCatching {
                    repository.insertTrip(trip)
                }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals("Database error", error?.message)
        }
}
