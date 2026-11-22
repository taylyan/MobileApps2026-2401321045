package com.huntersstory.app.data.repository

import com.huntersstory.app.data.local.entity.Equipment
import com.huntersstory.app.data.local.entity.HuntingDog
import com.huntersstory.app.data.local.entity.HuntingSpot
import com.huntersstory.app.data.local.entity.HuntingTrip
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val trips: List<HuntingTrip>,
    val spots: List<HuntingSpot>,
    val equipment: List<Equipment>,
    val dogs: List<HuntingDog>,
)

@Suppress("TooManyFunctions")
object CloudBackupCodec {
    const val SCHEMA_VERSION = 1
    const val MAX_PAYLOAD_BYTES = 900_000

    private const val MIN_DOG_AGE = 0
    private const val MAX_DOG_AGE = 30

    private const val MIN_LATITUDE = -90.0
    private const val MAX_LATITUDE = 90.0
    private const val MIN_LONGITUDE = -180.0
    private const val MAX_LONGITUDE = 180.0

    // ==================================
    // ENCODE
    // ==================================

    fun encode(data: BackupData): String {
        val root = JSONObject()

        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("trips", encodeTrips(data.trips))
        root.put("spots", encodeSpots(data.spots))
        root.put("equipment", encodeEquipment(data.equipment))
        root.put("dogs", encodeDogs(data.dogs))

        val payload = root.toString()
        checkSize(payload)

        return payload
    }

    private fun encodeTrips(trips: List<HuntingTrip>): JSONArray =
        JSONArray().apply {
            trips.forEach { trip ->
                put(
                    JSONObject().apply {
                        put("date", trip.date)
                        put("location", trip.location)
                        put("gameSpecies", trip.gameSpecies)
                        put("notes", trip.notes)
                    },
                )
            }
        }

    private fun encodeSpots(spots: List<HuntingSpot>): JSONArray =
        JSONArray().apply {
            spots.forEach { spot ->
                validateCoordinates(
                    spot.latitude,
                    spot.longitude,
                )

                put(
                    JSONObject().apply {
                        put("name", spot.name)
                        put("latitude", spot.latitude)
                        put("longitude", spot.longitude)
                        put("notes", spot.notes)
                    },
                )
            }
        }

    private fun encodeEquipment(equipment: List<Equipment>): JSONArray =
        JSONArray().apply {
            equipment.forEach { item ->
                put(
                    JSONObject().apply {
                        put("name", item.name)
                        put("category", item.category)
                        put("notes", item.notes)
                    },
                )
            }
        }

    private fun encodeDogs(dogs: List<HuntingDog>): JSONArray =
        JSONArray().apply {
            dogs.forEach { dog ->
                require(
                    dog.ageYears in MIN_DOG_AGE..MAX_DOG_AGE,
                ) {
                    "Invalid dog age."
                }

                put(
                    JSONObject().apply {
                        put("name", dog.name)
                        put("breed", dog.breed)
                        put("ageYears", dog.ageYears)
                        put("notes", dog.notes)
                    },
                )
            }
        }

    // ==================================
    // DECODE
    // ==================================

    fun decode(
        payload: String,
        uid: String,
    ): BackupData {
        require(uid.isNotBlank()) {
            "User ID is required."
        }

        checkSize(payload)

        val root = JSONObject(payload)

        require(
            root.getInt("schemaVersion") == SCHEMA_VERSION,
        ) {
            "Unsupported backup version."
        }

        return BackupData(
            trips =
                decodeTrips(
                    root.getJSONArray("trips"),
                    uid,
                ),
            spots =
                decodeSpots(
                    root.getJSONArray("spots"),
                    uid,
                ),
            equipment =
                decodeEquipment(
                    root.getJSONArray("equipment"),
                    uid,
                ),
            dogs =
                decodeDogs(
                    root.getJSONArray("dogs"),
                    uid,
                ),
        )
    }

    private fun decodeTrips(
        array: JSONArray,
        uid: String,
    ): List<HuntingTrip> =
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)

            HuntingTrip(
                userId = uid,
                date = item.getString("date"),
                location = item.getString("location"),
                gameSpecies = item.getString("gameSpecies"),
                notes = item.getString("notes"),
            )
        }

    private fun decodeSpots(
        array: JSONArray,
        uid: String,
    ): List<HuntingSpot> =
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)

            val latitude = item.getDouble("latitude")
            val longitude = item.getDouble("longitude")

            validateCoordinates(latitude, longitude)

            HuntingSpot(
                userId = uid,
                name = item.getString("name"),
                latitude = latitude,
                longitude = longitude,
                notes = item.getString("notes"),
            )
        }

    private fun decodeEquipment(
        array: JSONArray,
        uid: String,
    ): List<Equipment> =
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)

            Equipment(
                userId = uid,
                name = item.getString("name"),
                category = item.getString("category"),
                notes = item.getString("notes"),
            )
        }

    private fun decodeDogs(
        array: JSONArray,
        uid: String,
    ): List<HuntingDog> =
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)

            val age = item.getInt("ageYears")

            require(
                age in MIN_DOG_AGE..MAX_DOG_AGE,
            ) {
                "Invalid dog age."
            }

            HuntingDog(
                userId = uid,
                name = item.getString("name"),
                breed = item.getString("breed"),
                ageYears = age,
                notes = item.getString("notes"),
            )
        }

    // ==================================
    // VALIDATION
    // ==================================

    fun checkSize(payload: String) {
        require(
            payload.toByteArray(Charsets.UTF_8).size <
                MAX_PAYLOAD_BYTES,
        ) {
            "Cloud backup is too large."
        }
    }

    fun validateCoordinates(
        latitude: Double,
        longitude: Double,
    ) {
        require(
            latitude.isFinite() &&
                longitude.isFinite() &&
                latitude in MIN_LATITUDE..MAX_LATITUDE &&
                longitude in MIN_LONGITUDE..MAX_LONGITUDE,
        ) {
            "Invalid spot coordinates."
        }
    }
}
