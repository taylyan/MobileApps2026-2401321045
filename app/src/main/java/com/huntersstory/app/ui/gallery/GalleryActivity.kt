
package com.huntersstory.app.ui.gallery

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.huntersstory.app.R
import com.huntersstory.app.data.local.entity.HuntingPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class GalleryActivity : AppCompatActivity() {
    private lateinit var viewModel: GalleryViewModel
    private lateinit var adapter: GalleryAdapter

    private var pendingPhotoName: String? = null

    private val uid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture(),
        ) { success ->
            val fileName = pendingPhotoName
            pendingPhotoName = null

            if (fileName == null) {
                return@registerForActivityResult
            }

            val file = File(photoDirectory(), fileName)

            if (!success || !file.exists() || file.length() == 0L) {
                file.delete()
                message("Photo was not saved.")
                return@registerForActivityResult
            }

            val currentUid = uid ?: return@registerForActivityResult

            viewModel.save(
                uid = currentUid,
                fileName = fileName,
                onSuccess = {
                    message("Photo saved successfully!")
                },
                onError = { error ->
                    message(error)
                },
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (uid == null) {
            finish()
            return
        }

        pendingPhotoName =
            savedInstanceState?.getString("pending_photo")

        setContentView(R.layout.activity_gallery)

        findViewById<Button>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        viewModel =
            ViewModelProvider(this)[
                GalleryViewModel::class.java,
            ]

        val recycler =
            findViewById<RecyclerView>(R.id.rvGallery)
        val emptyText =
            findViewById<TextView>(R.id.tvGalleryEmpty)
        val takePhotoButton =
            findViewById<Button>(R.id.btnTakePhoto)

        adapter =
            GalleryAdapter(
                photoDirectory = photoDirectory(),
                scope = lifecycleScope,
                onView = { photo ->
                    showPhoto(photo)
                },
                onDelete = { photo ->
                    confirmDelete(photo)
                },
            )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        takePhotoButton.setOnClickListener {
            takePhoto()
        }

        val currentUid = uid ?: return

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observe(currentUid).collect { photos ->
                    adapter.submitList(photos)
                    emptyText.visibility =
                        if (photos.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }
            }
        }
    }

    private fun photoDirectory(): File =
        File(filesDir, "photos").apply {
            mkdirs()
        }

    // Camera launch may fail for several platform-specific reasons.
    @Suppress("TooGenericExceptionCaught")
    private fun takePhoto() {
        val fileName = "photo_${UUID.randomUUID()}.jpg"
        val file = File(photoDirectory(), fileName)

        try {
            val uri =
                FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    file,
                )

            pendingPhotoName = fileName
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            pendingPhotoName = null
            file.delete()
            message(
                e.localizedMessage ?: "Cannot open camera.",
            )
        }
    }

    private fun showPhoto(photo: HuntingPhoto) {
        val imageView =
            ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                setPadding(8, 8, 8, 8)
                minimumHeight =
                    (300 * resources.displayMetrics.density).toInt()
            }

        val dialog =
            AlertDialog
                .Builder(this)
                .setTitle("Hunting Photo")
                .setView(imageView)
                .setPositiveButton("Close", null)
                .create()

        dialog.show()

        lifecycleScope.launch {
            val bitmap =
                withContext(Dispatchers.IO) {
                    decodeSampledPhoto(
                        viewModel.photoFile(photo),
                        1200,
                    )
                }

            if (dialog.isShowing) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun confirmDelete(photo: HuntingPhoto) {
        val currentUid = uid ?: return

        AlertDialog
            .Builder(this)
            .setTitle("Delete Photo")
            .setMessage(
                "Are you sure you want to delete this photo?",
            ).setPositiveButton("Delete") { _, _ ->
                viewModel.delete(
                    photo = photo,
                    uid = currentUid,
                    onSuccess = {
                        message("Photo deleted.")
                    },
                    onError = { error ->
                        message(error)
                    },
                )
            }.setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            "pending_photo",
            pendingPhotoName,
        )
        super.onSaveInstanceState(outState)
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
