
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
import com.huntersstory.app.domain.validation.HuntingDogValidator
import kotlinx.coroutines.launch

class CollectionActivity : AppCompatActivity() {
    private companion object {
        const val MAX_DOG_AGE = 30
    }

    private lateinit var viewModel: CollectionViewModel
    private lateinit var adapter: CollectionAdapter
    private lateinit var type: CollectionType

    private val uid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    private data class DialogFields(
        val layout: LinearLayout,
        val name: EditText,
        val secondary: EditText,
        val age: EditText,
        val notes: EditText,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUid = uid
        if (currentUid == null) {
            finish()
            return
        }

        type =
            if (intent.getStringExtra("collection_type") == "dogs") {
                CollectionType.DOGS
            } else {
                CollectionType.EQUIPMENT
            }

        setContentView(R.layout.activity_collection)

        findViewById<Button>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        viewModel =
            ViewModelProvider(this)[
                CollectionViewModel::class.java,
            ]

        setupScreen(currentUid)
    }

    private fun setupScreen(currentUid: String) {
        val title =
            findViewById<TextView>(R.id.tvCollectionTitle)
        val addButton =
            findViewById<Button>(R.id.btnAddCollection)
        val recyclerView =
            findViewById<RecyclerView>(R.id.rvCollection)
        val emptyText =
            findViewById<TextView>(R.id.tvCollectionEmpty)

        val isDog = type == CollectionType.DOGS

        title.text = if (isDog) "My Dogs" else "My Equipment"
        addButton.text = if (isDog) "Add Dog" else "Add Equipment"

        adapter =
            CollectionAdapter(
                type = type,
                onEdit = { item -> showItemDialog(item) },
                onDelete = { item -> confirmDelete(item) },
            )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        addButton.setOnClickListener {
            showItemDialog(null)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observe(type, currentUid).collect { items ->
                    adapter.submitList(items)
                    emptyText.visibility =
                        if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showItemDialog(existing: CollectionItem?) {
        val currentUid = uid ?: return
        val isDog = type == CollectionType.DOGS

        val fields = createDialogFields(existing, isDog)

        val dialogTitle =
            when {
                existing != null && isDog -> "Edit Dog"
                existing != null -> "Edit Equipment"
                isDog -> "Add Dog"
                else -> "Add Equipment"
            }

        val dialog =
            AlertDialog
                .Builder(this)
                .setTitle(dialogTitle)
                .setView(fields.layout)
                .setPositiveButton(
                    if (existing == null) "Save" else "Update",
                    null,
                ).setNegativeButton("Cancel", null)
                .create()

        dialog.setOnShowListener {
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    submitDialog(fields, existing, currentUid, dialog)
                }
        }

        dialog.show()
    }

    private fun createDialogFields(
        existing: CollectionItem?,
        isDog: Boolean,
    ): DialogFields {
        val layout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val padding =
                    (20 * resources.displayMetrics.density).toInt()
                setPadding(padding, 8, padding, 0)
            }

        val name =
            EditText(this).apply {
                hint = if (isDog) "Dog name" else "Equipment name"
                inputType =
                    InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setText(existing?.name.orEmpty())
            }

        val secondary =
            EditText(this).apply {
                hint = if (isDog) "Breed" else "Category"
                inputType =
                    InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setText(existing?.secondary.orEmpty())
            }

        val age =
            EditText(this).apply {
                hint = "Age in years"
                inputType = InputType.TYPE_CLASS_NUMBER
                existing?.ageYears?.let { setText(it.toString()) }
                visibility = if (isDog) View.VISIBLE else View.GONE
            }

        val notes =
            EditText(this).apply {
                hint = "Notes (optional)"
                inputType =
                    InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 3
                setText(existing?.notes.orEmpty())
            }

        layout.addView(name)
        layout.addView(secondary)
        layout.addView(age)
        layout.addView(notes)

        return DialogFields(layout, name, secondary, age, notes)
    }

    private fun submitDialog(
        fields: DialogFields,
        existing: CollectionItem?,
        currentUid: String,
        dialog: AlertDialog,
    ) {
        val item = readDialogItem(fields, existing) ?: return
        val positiveButton =
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)

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
            },
        )
    }

    private fun readDialogItem(
        fields: DialogFields,
        existing: CollectionItem?,
    ): CollectionItem? {
        val name =
            fields.name.text
                .toString()
                .trim()
        val secondary =
            fields.secondary.text
                .toString()
                .trim()
        val notes =
            fields.notes.text
                .toString()
                .trim()

        val isDog = type == CollectionType.DOGS

        val age =
            if (isDog) {
                fields.age.text
                    .toString()
                    .toIntOrNull()
            } else {
                null
            }

        val dogValidationError =
            if (isDog && age != null && age in 0..MAX_DOG_AGE) {
                HuntingDogValidator.validate(
                    name = name,
                    breed = secondary,
                    age = age,
                )
            } else {
                null
            }

        return when {
            name.isBlank() -> {
                fields.name.error = "Required field"
                null
            }

            secondary.isBlank() -> {
                fields.secondary.error = "Required field"
                null
            }

            isDog && (age == null || age !in 0..MAX_DOG_AGE) -> {
                fields.age.error = "Enter an age from 0 to 30"
                null
            }

            dogValidationError != null -> {
                message(dogValidationError)
                null
            }

            else ->
                CollectionItem(
                    id = existing?.id ?: 0L,
                    name = name,
                    secondary = secondary,
                    notes = notes,
                    ageYears = age,
                )
        }
    }

    private fun confirmDelete(item: CollectionItem) {
        val currentUid = uid ?: return

        AlertDialog
            .Builder(this)
            .setTitle("Delete Record")
            .setMessage(
                "Are you sure you want to delete ${item.name}?",
            ).setPositiveButton("Delete") { _, _ ->
                viewModel.delete(
                    type = type,
                    uid = currentUid,
                    id = item.id,
                    onSuccess = {
                        message("Deleted successfully")
                    },
                    onError = { error ->
                        message(error)
                    },
                )
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun message(text: String) {
        Toast
            .makeText(
                this,
                text,
                Toast.LENGTH_SHORT,
            ).show()
    }
}
