
package com.huntersstory.app.ui.collection

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.huntersstory.app.R
import com.huntersstory.app.data.repository.CollectionItem
import com.huntersstory.app.data.repository.CollectionType
import kotlinx.coroutines.launch

class CollectionActivity : AppCompatActivity() {

    private lateinit var viewModel: CollectionViewModel
    private lateinit var adapter: CollectionAdapter
    private lateinit var type: CollectionType

    private val uid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUid = uid
        if (currentUid == null) {
            finish()
            return
        }

        type = if (
            intent.getStringExtra("collection_type") == "dogs"
        ) {
            CollectionType.DOGS
        } else {
            CollectionType.EQUIPMENT
        }

        setContentView(R.layout.activity_collection)

        viewModel = ViewModelProvider(this)[
            CollectionViewModel::class.java
        ]

        val title = findViewById<TextView>(
            R.id.tvCollectionTitle
        )
        val addButton = findViewById<Button>(
            R.id.btnAddCollection
        )
        val recyclerView = findViewById<RecyclerView>(
            R.id.rvCollection
        )
        val emptyText = findViewById<TextView>(
            R.id.tvCollectionEmpty
        )

        if (type == CollectionType.EQUIPMENT) {
            title.text = "My Equipment"
            addButton.text = "Add Equipment"
        } else {
            title.text = "My Dogs"
            addButton.text = "Add Dog"
        }

        adapter = CollectionAdapter(
            type = type,
            onEdit = { item ->
                showItemDialog(item)
            },
            onDelete = { item ->
                confirmDelete(item)
            }
        )

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            showItemDialog(null)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observe(type, currentUid)
                    .collect { items ->

                        adapter.submitList(items)

                        emptyText.visibility =
                            if (items.isEmpty()) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }
            }
        }
    }

    private fun showItemDialog(existing: CollectionItem?) {
        val currentUid = uid ?: return
        val isDog = type == CollectionType.DOGS

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density)
                .toInt()
            setPadding(padding, 8, padding, 0)
        }

        val nameField = EditText(this).apply {
            hint = if (isDog) "Dog name" else "Equipment name"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(existing?.name.orEmpty())
        }

        val secondaryField = EditText(this).apply {
            hint = if (isDog) "Breed" else "Category"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(existing?.secondary.orEmpty())
        }

        val ageField = EditText(this).apply {
            hint = "Age in years"
            inputType = InputType.TYPE_CLASS_NUMBER
            if (existing?.ageYears != null) {
                setText(existing.ageYears.toString())
            }
            visibility = if (isDog) View.VISIBLE else View.GONE
        }

        val notesField = EditText(this).apply {
            hint = "Notes (optional)"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            setText(existing?.notes.orEmpty())
        }

        layout.addView(nameField)
        layout.addView(secondaryField)
        layout.addView(ageField)
        layout.addView(notesField)

        val dialogTitle = when {
            existing != null && isDog -> "Edit Dog"
            existing != null -> "Edit Equipment"
            isDog -> "Add Dog"
            else -> "Add Equipment"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setView(layout)
            .setPositiveButton(
                if (existing == null) "Save" else "Update",
                null
            )
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

            positiveButton.setOnClickListener {
                val name = nameField.text.toString().trim()
                val secondary = secondaryField.text
                    .toString().trim()
                val notes = notesField.text.toString().trim()

                if (name.isBlank()) {
                    nameField.error = "Required field"
                    return@setOnClickListener
                }

                if (secondary.isBlank()) {
                    secondaryField.error = "Required field"
                    return@setOnClickListener
                }

                val age: Int? = if (isDog) {
                    val enteredAge = ageField.text
                        .toString().toIntOrNull()

                    if (enteredAge == null ||
                        enteredAge !in 0..30
                    ) {
                        ageField.error =
                            "Enter an age from 0 to 30"
                        return@setOnClickListener
                    }

                    enteredAge
                } else {
                    null
                }

                val item = CollectionItem(
                    id = existing?.id ?: 0L,
                    name = name,
                    secondary = secondary,
                    notes = notes,
                    ageYears = age
                )

                positiveButton.isEnabled = false

                viewModel.save(
                    type = type,
                    uid = currentUid,
                    item = item,
                    onSuccess = {
                        message("Saved successfully")
                        dialog.dismiss()
                    },
                    onError = { error ->
                        positiveButton.isEnabled = true
                        message(error)
                    }
                )
            }
        }

        dialog.show()
    }

    private fun confirmDelete(item: CollectionItem) {
        val currentUid = uid ?: return

        AlertDialog.Builder(this)
            .setTitle("Delete Record")
            .setMessage(
                "Are you sure you want to delete ${item.name}?"
            )
            .setPositiveButton("Delete") { _, _ ->
                viewModel.delete(
                    type = type,
                    uid = currentUid,
                    id = item.id,
                    onSuccess = {
                        message("Deleted successfully")
                    },
                    onError = { error ->
                        message(error)
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun message(text: String) {
        Toast.makeText(
            this,
            text,
            Toast.LENGTH_SHORT
        ).show()
    }
}
