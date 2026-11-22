
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
import com.huntersstory.app.ui.journal.viewmodel.HuntingTripViewModel
import kotlinx.coroutines.launch
import com.huntersstory.app.domain.validation.HuntingTripValidator

class AddHuntActivity : AppCompatActivity() {

    private lateinit var viewModel: HuntingTripViewModel
    private var existingTrip: HuntingTrip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hunt)

        viewModel =
            ViewModelProvider(this)[HuntingTripViewModel::class.java]

        val dateField = findViewById<EditText>(R.id.etHuntDate)
        val locationField = findViewById<EditText>(R.id.etHuntLocation)
        val speciesField = findViewById<EditText>(R.id.etGameSpecies)
        val notesField = findViewById<EditText>(R.id.etHuntNotes)
        val saveButton = findViewById<Button>(R.id.btnSaveHunt)

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Toast.makeText(
                this, "Please log in again.", Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        val tripId = intent.getLongExtra("trip_id", -1L)

        if (tripId != -1L) {
            saveButton.isEnabled = false

            lifecycleScope.launch {
                val trip = HunterDatabase.getDatabase(
                    applicationContext
                ).huntingTripDao().getTripById(tripId, userId)

                if (trip == null) {
                    Toast.makeText(
                        this@AddHuntActivity,
                        "Hunting trip not found.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                existingTrip = trip

                dateField.setText(trip.date)
                locationField.setText(trip.location)
                speciesField.setText(trip.gameSpecies)
                notesField.setText(trip.notes)

                saveButton.text = "Update Hunting Trip"
                saveButton.isEnabled = true
            }
        }

        saveButton.setOnClickListener {
            val date = dateField.text.toString().trim()
            val location = locationField.text.toString().trim()
            val species = speciesField.text.toString().trim()
            val notes = notesField.text.toString().trim()


            val validationError = HuntingTripValidator.validate(
                date = date,
                location = location,
                species = species
            )

            if (validationError != null) {
                Toast.makeText(
                    this,
                    validationError,
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            val trip = HuntingTrip(
                id = existingTrip?.id ?: 0L,
                userId = userId,
                date = date,
                location = location,
                gameSpecies = species,
                notes = notes
            )

            saveButton.isEnabled = false

            if (existingTrip == null) {
                viewModel.saveTrip(
                    trip,
                    onSuccess = {
                        Toast.makeText(
                            this,
                            "Hunting trip saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    },
                    onError = { error ->
                        saveButton.isEnabled = true
                        Toast.makeText(
                            this, error, Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } else {
                lifecycleScope.launch {
                    try {
                        HunterDatabase.getDatabase(
                            applicationContext
                        ).huntingTripDao().update(trip)

                        Toast.makeText(
                            this@AddHuntActivity,
                            "Hunting trip updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } catch (e: Exception) {
                        saveButton.isEnabled = true
                        Toast.makeText(
                            this@AddHuntActivity,
                            e.localizedMessage ?: "Update failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}
