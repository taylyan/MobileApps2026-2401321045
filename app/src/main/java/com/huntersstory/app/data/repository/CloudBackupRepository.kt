
package com.huntersstory.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.huntersstory.app.data.local.HunterDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

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
        require(uid.isNotBlank()) {
            "User ID is required."
        }

        val data = BackupData(
            trips = db.huntingTripDao()
                .getTripsByUser(uid).first(),
            spots = db.huntingSpotDao()
                .getSpotsByUser(uid).first(),
            equipment = db.equipmentDao()
                .getByUser(uid).first(),
            dogs = db.huntingDogDao()
                .getByUser(uid).first()
        )

        val payload = CloudBackupCodec.encode(data)

        backupDocument(uid).set(
            mapOf(
                "schemaVersion" to
                        CloudBackupCodec.SCHEMA_VERSION,
                "payload" to payload,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun restore(uid: String) {
        require(uid.isNotBlank()) {
            "User ID is required."
        }

        val snapshot = backupDocument(uid)
            .get(Source.SERVER)
            .await()

        require(snapshot.exists()) {
            "No cloud backup found for this account."
        }

        val payload = snapshot.getString("payload")
            ?: error("Invalid cloud backup.")

        // Decode and validate BEFORE editing the database.
        val data = CloudBackupCodec.decode(payload, uid)

        // All local changes happen in one transaction.
        db.withTransaction {
            db.huntingTripDao().deleteAllForUser(uid)
            db.huntingSpotDao().deleteAllForUser(uid)
            db.equipmentDao().deleteAllForUser(uid)
            db.huntingDogDao().deleteAllForUser(uid)

            data.trips.forEach {
                db.huntingTripDao().insert(it)
            }

            data.spots.forEach {
                db.huntingSpotDao().insert(it)
            }

            data.equipment.forEach {
                db.equipmentDao().insert(it)
            }

            data.dogs.forEach {
                db.huntingDogDao().insert(it)
            }
        }
    }
}
