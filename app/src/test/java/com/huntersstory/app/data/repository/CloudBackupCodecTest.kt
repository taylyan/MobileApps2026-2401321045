
package com.huntersstory.app.data.repository

import com.huntersstory.app.data.local.entity.Equipment
import com.huntersstory.app.data.local.entity.HuntingDog
import com.huntersstory.app.data.local.entity.HuntingSpot
import com.huntersstory.app.data.local.entity.HuntingTrip
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBackupCodecTest {
    private val uid = "test-user-123"

    private fun sampleData() =
        BackupData(
            trips =
                listOf(
                    HuntingTrip(
                        id = 5,
                        userId = uid,
                        date = "2026-11-22",
                        location = "Kuklen",
                        gameSpecies = "Pheasant",
                        notes = "Morning trip",
                    ),
                ),
            spots =
                listOf(
                    HuntingSpot(
                        id = 8,
                        userId = uid,
                        name = "Forest spot",
                        latitude = 42.035,
                        longitude = 24.786,
                        notes = "Near the trees",
                    ),
                ),
            equipment =
                listOf(
                    Equipment(
                        id = 3,
                        userId = uid,
                        name = "Binoculars",
                        category = "Optics",
                        notes = "10x42",
                    ),
                ),
            dogs =
                listOf(
                    HuntingDog(
                        id = 7,
                        userId = uid,
                        name = "Rex",
                        breed = "Pointer",
                        ageYears = 3,
                        notes = "Trained",
                    ),
                ),
        )

    private fun emptyData() =
        BackupData(
            trips = emptyList(),
            spots = emptyList(),
            equipment = emptyList(),
            dogs = emptyList(),
        )

    @Test
    fun backupContainsCorrectSchemaVersion() {
        val json =
            JSONObject(
                CloudBackupCodec.encode(sampleData()),
            )

        assertEquals(1, json.getInt("schemaVersion"))
    }

    @Test
    fun backupContainsAllFourCollections() {
        val json =
            JSONObject(
                CloudBackupCodec.encode(sampleData()),
            )

        assertEquals(1, json.getJSONArray("trips").length())
        assertEquals(1, json.getJSONArray("spots").length())
        assertEquals(
            1,
            json.getJSONArray("equipment").length(),
        )
        assertEquals(1, json.getJSONArray("dogs").length())
    }

    @Test
    fun roundTripPreservesHuntingTrip() {
        val json = CloudBackupCodec.encode(sampleData())
        val result = CloudBackupCodec.decode(json, uid)

        val trip = result.trips.single()

        assertEquals("Kuklen", trip.location)
        assertEquals("Pheasant", trip.gameSpecies)
        assertEquals("Morning trip", trip.notes)
        assertEquals("2026-11-22", trip.date)
    }

    @Test
    fun roundTripPreservesSpotCoordinates() {
        val json = CloudBackupCodec.encode(sampleData())
        val result = CloudBackupCodec.decode(json, uid)

        val spot = result.spots.single()

        assertEquals(42.035, spot.latitude, 0.000001)
        assertEquals(24.786, spot.longitude, 0.000001)
        assertEquals("Forest spot", spot.name)
    }

    @Test
    fun roundTripPreservesEquipment() {
        val json = CloudBackupCodec.encode(sampleData())
        val result = CloudBackupCodec.decode(json, uid)

        val item = result.equipment.single()

        assertEquals("Binoculars", item.name)
        assertEquals("Optics", item.category)
        assertEquals("10x42", item.notes)
    }

    @Test
    fun roundTripPreservesDog() {
        val json = CloudBackupCodec.encode(sampleData())
        val result = CloudBackupCodec.decode(json, uid)

        val dog = result.dogs.single()

        assertEquals("Rex", dog.name)
        assertEquals("Pointer", dog.breed)
        assertEquals(3, dog.ageYears)
    }

    @Test
    fun restoredDataUsesCurrentUserId() {
        val json = CloudBackupCodec.encode(sampleData())
        val result =
            CloudBackupCodec.decode(
                json,
                "another-user",
            )

        assertEquals("another-user", result.trips[0].userId)
        assertEquals("another-user", result.spots[0].userId)
        assertEquals(
            "another-user",
            result.equipment[0].userId,
        )
        assertEquals("another-user", result.dogs[0].userId)
    }

    @Test
    fun restoredRecordsReceiveNewLocalIds() {
        val json = CloudBackupCodec.encode(sampleData())
        val result = CloudBackupCodec.decode(json, uid)

        assertEquals(0L, result.trips[0].id)
        assertEquals(0L, result.spots[0].id)
        assertEquals(0L, result.equipment[0].id)
        assertEquals(0L, result.dogs[0].id)
    }

    @Test
    fun emptyBackupIsSupported() {
        val json = CloudBackupCodec.encode(emptyData())
        val result = CloudBackupCodec.decode(json, uid)

        assertTrue(result.trips.isEmpty())
        assertTrue(result.spots.isEmpty())
        assertTrue(result.equipment.isEmpty())
        assertTrue(result.dogs.isEmpty())
    }

    @Test
    fun unsupportedSchemaIsRejected() {
        val json =
            JSONObject(
                CloudBackupCodec.encode(emptyData()),
            )

        json.put("schemaVersion", 999)

        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.decode(json.toString(), uid)
        }
    }

    @Test
    fun emptyUserIdIsRejected() {
        val json = CloudBackupCodec.encode(emptyData())

        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.decode(json, "")
        }
    }

    @Test
    fun invalidLatitudeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.validateCoordinates(
                95.0,
                24.0,
            )
        }
    }

    @Test
    fun invalidLongitudeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.validateCoordinates(
                42.0,
                200.0,
            )
        }
    }

    @Test
    fun validBoundaryCoordinatesAreAccepted() {
        CloudBackupCodec.validateCoordinates(-90.0, -180.0)
        CloudBackupCodec.validateCoordinates(90.0, 180.0)
    }

    @Test
    fun nonFiniteCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.validateCoordinates(
                Double.NaN,
                24.0,
            )
        }
    }

    @Test
    fun oversizedPayloadIsRejected() {
        val oversized =
            "a".repeat(
                CloudBackupCodec.MAX_PAYLOAD_BYTES,
            )

        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.checkSize(oversized)
        }
    }

    @Test
    fun oversizedUnicodePayloadIsRejected() {
        val oversized = "🦌".repeat(230_000)

        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.checkSize(oversized)
        }
    }

    @Test
    fun invalidDogAgeIsRejectedOnUpload() {
        val data =
            emptyData().copy(
                dogs =
                    listOf(
                        HuntingDog(
                            userId = uid,
                            name = "Rex",
                            breed = "Pointer",
                            ageYears = -2,
                        ),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.encode(data)
        }
    }

    @Test
    fun invalidDogAgeIsRejectedOnRestore() {
        val json =
            JSONObject(
                CloudBackupCodec.encode(sampleData()),
            )

        json
            .getJSONArray("dogs")
            .getJSONObject(0)
            .put("ageYears", 99)

        assertThrows(IllegalArgumentException::class.java) {
            CloudBackupCodec.decode(json.toString(), uid)
        }
    }

    @Test
    fun missingCollectionsAreRejected() {
        val json =
            JSONObject(
                CloudBackupCodec.encode(emptyData()),
            )

        json.remove("equipment")

        assertThrows(Exception::class.java) {
            CloudBackupCodec.decode(json.toString(), uid)
        }
    }

    @Test
    fun specialCharactersArePreserved() {
        val data =
            emptyData().copy(
                trips =
                    listOf(
                        HuntingTrip(
                            userId = uid,
                            date = "2026-11-22",
                            location = "Родопите",
                            gameSpecies = "Quail",
                            notes = "Forest \"A\"\nMorning",
                        ),
                    ),
            )

        val result =
            CloudBackupCodec.decode(
                CloudBackupCodec.encode(data),
                uid,
            )

        assertEquals("Родопите", result.trips[0].location)
        assertEquals(
            "Forest \"A\"\nMorning",
            result.trips[0].notes,
        )
    }
}
