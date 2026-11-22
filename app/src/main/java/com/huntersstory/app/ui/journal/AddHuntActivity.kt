
package com.huntersstory.app.ui.journal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.huntersstory.app.R
import com.huntersstory.app.data.local.HunterDatabase
import com.huntersstory.app.data.local.entity.HuntingTrip
import com.huntersstory.app.domain.validation.HuntingTripValidator
import com.huntersstory.app.ui.journal.viewmodel.HuntingTripViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AddHuntActivity : AppCompatActivity() {
    private lateinit var viewModel: HuntingTripViewModel
    private var existingTrip: HuntingTrip? = null

    private data class HuntFields(
        val date: EditText,
        val location: EditText,
        val species: EditText,
        val notes: EditText,
        val saveButton: Button,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hunt)

        findViewById<Button>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        viewModel =
            ViewModelProvider(this)[
                HuntingTripViewModel::class.java,
            ]

        val userId =
            FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            message("Please log in again.")
            finish()
            return
        }

        val fields = findHuntFields()
        val tripId = intent.getLongExtra("trip_id", -1L)

        if (tripId != -1L) {
            loadExistingTrip(tripId, userId, fields)
        }

        fields.saveButton.setOnClickListener {
            submitTrip(userId, fields)
        }
    }

    private fun findHuntFields(): HuntFields =
        HuntFields(
            date = findViewById(R.id.etHuntDate),
            location = findViewById(R.id.etHuntLocation),
            species = findViewById(R.id.etGameSpecies),
            notes = findViewById(R.id.etHuntNotes),
            saveButton = findViewById(R.id.btnSaveHunt),
        )

    private fun loadExistingTrip(
        tripId: Long,
        userId: String,
        fields: HuntFields,
    ) {
        fields.saveButton.isEnabled = false

        lifecycleScope.launch {
            val trip =
                HunterDatabase
                    .getDatabase(applicationContext)
                    .huntingTripDao()
                    .getTripById(tripId, userId)

            if (trip == null) {
                message("Hunting trip not found.")
                finish()
                return@launch
            }

            existingTrip = trip

            fields.date.setText(trip.date)
            fields.location.setText(trip.location)
            fields.species.setText(trip.gameSpecies)
            fields.notes.setText(trip.notes)

            fields.saveButton.text = "Update Hunting Trip"
            fields.saveButton.isEnabled = true
        }
    }

    private fun submitTrip(
        userId: String,
        fields: HuntFields,
    ) {
        val date =
            fields.date.text
                .toString()
                .trim()
        val location =
            fields.location.text
                .toString()
                .trim()
        val species =
            fields.species.text
                .toString()
                .trim()
        val notes =
            fields.notes.text
                .toString()
                .trim()

        val validationError =
            HuntingTripValidator.validate(
                date = date,
                location = location,
                species = species,
            )

        if (validationError != null) {
            message(validationError)
            return
        }

        val trip =
            HuntingTrip(
                id = existingTrip?.id ?: 0L,
                userId = userId,
                date = date,
                location = location,
                gameSpecies = species,
                notes = notes,
            )

        fields.saveButton.isEnabled = false

        if (existingTrip == null) {
            createTrip(trip, fields.saveButton)
        } else {
            updateExistingTrip(trip, fields.saveButton)
        }
    }

    private fun createTrip(
        trip: HuntingTrip,
        saveButton: Button,
    ) {
        viewModel.saveTrip(
            trip,
            onSuccess = {
                message("Hunting trip saved successfully!")
                finish()
            },
            onError = { error ->
                saveButton.isEnabled = true
                message(error, Toast.LENGTH_LONG)
            },
        )
    }

    // Database update errors are shown to the user.
    // Coroutine cancellation is not treated as a failure.
    @Suppress("TooGenericExceptionCaught")
    private fun updateExistingTrip(
        trip: HuntingTrip,
        saveButton: Button,
    ) {
        lifecycleScope.launch {
            try {
                HunterDatabase
                    .getDatabase(applicationContext)
                    .huntingTripDao()
                    .update(trip)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saveButton.isEnabled = true
                message(
                    e.localizedMessage ?: "Update failed.",
                    Toast.LENGTH_LONG,
                )
                return@launch
            }

            message("Hunting trip updated successfully!")
            finish()
        }
    }

    private fun message(
        text: String,
        duration: Int = Toast.LENGTH_SHORT,
    ) {
        Toast
            .makeText(
                this,
                text,
                duration,
            ).show()
    }
}
