
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
    val dogs: List<HuntingDog>
)

object CloudBackupCodec {

    const val SCHEMA_VERSION = 1
    const val MAX_PAYLOAD_BYTES = 900_000

    fun encode(data: BackupData): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)

        root.put("trips", JSONArray().apply {
            data.trips.forEach { trip ->
                put(JSONObject().apply {
                    put("date", trip.date)
                    put("location", trip.location)
                    put("gameSpecies", trip.gameSpecies)
                    put("notes", trip.notes)
                })
            }
        })

        root.put("spots", JSONArray().apply {
            data.spots.forEach { spot ->
                validateCoordinates(
                    spot.latitude,
                    spot.longitude
                )

                put(JSONObject().apply {
                    put("name", spot.name)
                    put("latitude", spot.latitude)
                    put("longitude", spot.longitude)
                    put("notes", spot.notes)
                })
            }
        })

        root.put("equipment", JSONArray().apply {
            data.equipment.forEach { item ->
                put(JSONObject().apply {
                    put("name", item.name)
                    put("category", item.category)
                    put("notes", item.notes)
                })
            }
        })

        root.put("dogs", JSONArray().apply {
            data.dogs.forEach { dog ->
                require(dog.ageYears in 0..30) {
                    "Invalid dog age."
                }

                put(JSONObject().apply {
                    put("name", dog.name)
                    put("breed", dog.breed)
                    put("ageYears", dog.ageYears)
                    put("notes", dog.notes)
                })
            }
        })

        val payload = root.toString()
        checkSize(payload)
        return payload
    }

    fun decode(payload: String, uid: String): BackupData {
        require(uid.isNotBlank()) {
            "User ID is required."
        }

        checkSize(payload)

        val root = JSONObject(payload)

        require(
            root.getInt("schemaVersion") == SCHEMA_VERSION
        ) {
            "Unsupported backup version."
        }

        val tripArray = root.getJSONArray("trips")
        val spotArray = root.getJSONArray("spots")
        val equipmentArray = root.getJSONArray("equipment")
        val dogArray = root.getJSONArray("dogs")

        val trips = (0 until tripArray.length()).map { i ->
            val item = tripArray.getJSONObject(i)

            HuntingTrip(
                userId = uid,
                date = item.getString("date"),
                location = item.getString("location"),
                gameSpecies = item.getString("gameSpecies"),
                notes = item.getString("notes")
            )
        }

        val spots = (0 until spotArray.length()).map { i ->
            val item = spotArray.getJSONObject(i)

            val latitude = item.getDouble("latitude")
            val longitude = item.getDouble("longitude")

            validateCoordinates(latitude, longitude)

            HuntingSpot(
                userId = uid,
                name = item.getString("name"),
                latitude = latitude,
                longitude = longitude,
                notes = item.getString("notes")
            )
        }

        val equipment = (
                0 until equipmentArray.length()
                ).map { i ->
                val item = equipmentArray.getJSONObject(i)

                Equipment(
                    userId = uid,
                    name = item.getString("name"),
                    category = item.getString("category"),
                    notes = item.getString("notes")
                )
            }

        val dogs = (0 until dogArray.length()).map { i ->
            val item = dogArray.getJSONObject(i)

            val age = item.getInt("ageYears")

            require(age in 0..30) {
                "Invalid dog age."
            }

            HuntingDog(
                userId = uid,
                name = item.getString("name"),
                breed = item.getString("breed"),
                ageYears = age,
                notes = item.getString("notes")
            )
        }

        return BackupData(
            trips = trips,
            spots = spots,
            equipment = equipment,
            dogs = dogs
        )
    }

    fun checkSize(payload: String) {
        require(
            payload.toByteArray(Charsets.UTF_8).size <
                    MAX_PAYLOAD_BYTES
        ) {
            "Cloud backup is too large."
        }
    }

    fun validateCoordinates(
        latitude: Double,
        longitude: Double
    ) {
        require(
            latitude.isFinite() &&
                    longitude.isFinite() &&
                    latitude in -90.0..90.0 &&
                    longitude in -180.0..180.0
        ) {
            "Invalid spot coordinates."
        }
    }
}
