
package com.huntersstory.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.FieldValue
import com.huntersstory.app.data.local.HunterDatabase
import com.huntersstory.app.data.local.entity.Equipment
import com.huntersstory.app.data.local.entity.HuntingDog
import com.huntersstory.app.data.local.entity.HuntingSpot
import com.huntersstory.app.data.local.entity.HuntingTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class CloudBackupRepository(context: Context) {

    private val db = HunterDatabase.getDatabase(
        context.applicationContext
    )

    private val firestore = FirebaseFirestore.getInstance()

    private fun backupDocument(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("backups")
            .document("latest")

    suspend fun upload(uid: String) {

        val trips = db.huntingTripDao()
            .getTripsByUser(uid).first()

        val spots = db.huntingSpotDao()
            .getSpotsByUser(uid).first()

        val equipment = db.equipmentDao()
            .getByUser(uid).first()

        val dogs = db.huntingDogDao()
            .getByUser(uid).first()

        val root = JSONObject()
        root.put("schemaVersion", 1)

        val tripsArray = JSONArray()
        trips.forEach { trip ->
            tripsArray.put(JSONObject().apply {
                put("date", trip.date)
                put("location", trip.location)
                put("gameSpecies", trip.gameSpecies)
                put("notes", trip.notes)
            })
        }
        root.put("trips", tripsArray)

        val spotsArray = JSONArray()
        spots.forEach { spot ->
            spotsArray.put(JSONObject().apply {
                put("name", spot.name)
                put("latitude", spot.latitude)
                put("longitude", spot.longitude)
                put("notes", spot.notes)
            })
        }
        root.put("spots", spotsArray)

        val equipmentArray = JSONArray()
        equipment.forEach { item ->
            equipmentArray.put(JSONObject().apply {
                put("name", item.name)
                put("category", item.category)
                put("notes", item.notes)
            })
        }
        root.put("equipment", equipmentArray)

        val dogsArray = JSONArray()
        dogs.forEach { dog ->
            dogsArray.put(JSONObject().apply {
                put("name", dog.name)
                put("breed", dog.breed)
                put("ageYears", dog.ageYears)
                put("notes", dog.notes)
            })
        }
        root.put("dogs", dogsArray)

        val payload = root.toString()

        require(payload.toByteArray(Charsets.UTF_8).size < 900_000) {
            "Backup is too large for one Firestore document."
        }

        backupDocument(uid).set(
            mapOf(
                "schemaVersion" to 1,
                "payload" to payload,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun restore(uid: String) {

        // Require an online server response, not cached data.
        val snapshot = backupDocument(uid)
            .get(Source.SERVER).await()

        require(snapshot.exists()) {
            "No cloud backup found for this account."
        }

        val payload = snapshot.getString("payload")
            ?: error("Invalid cloud backup.")

        val root = JSONObject(payload)

        require(root.getInt("schemaVersion") == 1) {
            "Unsupported backup version."
        }

        val tripsArray = root.getJSONArray("trips")
        val spotsArray = root.getJSONArray("spots")
        val equipmentArray = root.getJSONArray("equipment")
        val dogsArray = root.getJSONArray("dogs")

        // Parse and validate everything BEFORE modifying Room.

        val trips = (0 until tripsArray.length()).map { i ->
            val item = tripsArray.getJSONObject(i)
            HuntingTrip(
                userId = uid,
                date = item.getString("date"),
                location = item.getString("location"),
                gameSpecies = item.getString("gameSpecies"),
                notes = item.getString("notes")
            )
        }

        val spots = (0 until spotsArray.length()).map { i ->
            val item = spotsArray.getJSONObject(i)
            val latitude = item.getDouble("latitude")
            val longitude = item.getDouble("longitude")

            require(
                latitude.isFinite() &&
                        longitude.isFinite() &&
                        latitude in -90.0..90.0 &&
                        longitude in -180.0..180.0
            ) {
                "Invalid spot coordinates."
            }

            HuntingSpot(
                userId = uid,
                name = item.getString("name"),
                latitude = latitude,
                longitude = longitude,
                notes = item.getString("notes")
            )
        }

        val equipment = (0 until equipmentArray.length()).map { i ->
            val item = equipmentArray.getJSONObject(i)
            Equipment(
                userId = uid,
                name = item.getString("name"),
                category = item.getString("category"),
                notes = item.getString("notes")
            )
        }

        val dogs = (0 until dogsArray.length()).map { i ->
            val item = dogsArray.getJSONObject(i)
            val age = item.getInt("ageYears")

            require(age in 0..30) {
                "Invalid dog age in backup."
            }

            HuntingDog(
                userId = uid,
                name = item.getString("name"),
                breed = item.getString("breed"),
                ageYears = age,
                notes = item.getString("notes")
            )
        }

        db.withTransaction {

            db.huntingTripDao().deleteAllForUser(uid)
            db.huntingSpotDao().deleteAllForUser(uid)
            db.equipmentDao().deleteAllForUser(uid)
            db.huntingDogDao().deleteAllForUser(uid)

            trips.forEach {
                db.huntingTripDao().insert(it)
            }

            spots.forEach {
                db.huntingSpotDao().insert(it)
            }

            equipment.forEach {
                db.equipmentDao().insert(it)
            }

            dogs.forEach {
                db.huntingDogDao().insert(it)
            }
        }
    }
}
