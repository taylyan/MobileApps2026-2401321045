package com.huntersstory.app.ui.journal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.huntersstory.app.R

class AddHuntActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hunt)

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

            Toast.makeText(
                this,
                "Form validated. Saving will be added next.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
