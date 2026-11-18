
package com.huntersstory.app.ui.journal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.huntersstory.app.R
import com.huntersstory.app.data.local.entity.HuntingTrip
import com.huntersstory.app.ui.journal.viewmodel.HuntingTripViewModel

class AddHuntActivity : AppCompatActivity() {

    private lateinit var viewModel: HuntingTripViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hunt)

        viewModel = ViewModelProvider(this)[HuntingTripViewModel::class.java]

        val date = findViewById<EditText>(R.id.etHuntDate)
        val location = findViewById<EditText>(R.id.etHuntLocation)
        val species = findViewById<EditText>(R.id.etGameSpecies)
        val notes = findViewById<EditText>(R.id.etHuntNotes)
        val saveButton = findViewById<Button>(R.id.btnSaveHunt)

        saveButton.setOnClickListener {

            val dateText = date.text.toString().trim()
            val locationText = location.text.toString().trim()
            val speciesText = species.text.toString().trim()
            val notesText = notes.text.toString().trim()

            if (dateText.isEmpty()) {
                date.error = "Date is required"
                return@setOnClickListener
            }

            if (locationText.isEmpty()) {
                location.error = "Location is required"
                return@setOnClickListener
            }

            if (speciesText.isEmpty()) {
                species.error = "Game species is required"
                return@setOnClickListener
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId == null) {
                Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val trip = HuntingTrip(
                userId = userId,
                date = dateText,
                location = locationText,
                gameSpecies = speciesText,
                notes = notesText
            )

            saveButton.isEnabled = false

            viewModel.saveTrip(
                trip = trip,
                onSuccess = {
                    Toast.makeText(
                        this,
                        "Hunting trip saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                },
                onError = { message ->
                    saveButton.isEnabled = true
                    Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}
