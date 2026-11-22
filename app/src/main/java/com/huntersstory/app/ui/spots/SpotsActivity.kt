
package com.huntersstory.app.ui.spots

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.huntersstory.app.R
import com.huntersstory.app.data.local.HunterDatabase
import com.huntersstory.app.data.local.entity.HuntingSpot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale

// Includes MapLibre lifecycle callbacks as well as map interactions.
@Suppress("TooManyFunctions")
class SpotsActivity : AppCompatActivity() {
    private companion object {
        const val DEFAULT_ZOOM = 11.0
        const val LOCATION_ZOOM = 16.0
        const val MAP_STYLE_URL =
            "https://tiles.openfreemap.org/styles/liberty"
    }

    private data class SaveFields(
        val layout: LinearLayout,
        val name: EditText,
        val notes: EditText,
    )

    private lateinit var mapView: MapView
    private lateinit var map: MapLibreMap
    private lateinit var selectedText: TextView
    private lateinit var saveButton: Button

    private val database by lazy {
        HunterDatabase.getDatabase(applicationContext)
    }

    private val locationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var selectedLocation: LatLng? = null
    private var selectedMarker: Marker? = null

    private val savedMarkers = mutableListOf<Marker>()
    private val markerSpots = mutableMapOf<Long, HuntingSpot>()

    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            if (permissions.values.any { it }) {
                moveToMyLocation()
            } else {
                message("Location permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (userId == null) {
            finish()
            return
        }

        MapLibre.getInstance(this)
        setContentView(R.layout.activity_spots)

        findViewById<Button>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        selectedText = findViewById(R.id.tvSelectedSpot)
        saveButton = findViewById(R.id.btnSaveSpot)

        mapView = MapView(this)

        findViewById<FrameLayout>(
            R.id.mapContainer,
        ).addView(mapView)

        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { readyMap ->
            map = readyMap

            map.setStyle(MAP_STYLE_URL) {
                setupMap()
                observeSpots()
            }
        }

        findViewById<Button>(R.id.btnMyLocation)
            .setOnClickListener {
                requestLocation()
            }

        saveButton.setOnClickListener {
            showSaveDialog()
        }
    }

    private fun setupMap() {
        val plovdiv = LatLng(42.1354, 24.7453)

        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                plovdiv,
                DEFAULT_ZOOM,
            ),
        )

        map.addOnMapLongClickListener { point ->
            selectLocation(point)
            true
        }

        map.setOnMarkerClickListener { marker ->
            val spot = markerSpots[marker.id]

            if (spot != null) {
                showSpotDetails(spot)
            } else {
                map.selectMarker(marker)
            }

            true
        }
    }

    private fun selectLocation(point: LatLng) {
        selectedLocation = point

        selectedMarker?.let {
            map.removeMarker(it)
        }

        selectedMarker =
            map.addMarker(
                MarkerOptions()
                    .position(point)
                    .title("Selected location"),
            )

        selectedText.text =
            String.format(
                Locale.US,
                "Selected: %.6f, %.6f",
                point.latitude,
                point.longitude,
            )

        saveButton.isEnabled = true
    }

    private fun hasLocationPermission(): Boolean {
        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    private fun requestLocation() {
        if (hasLocationPermission()) {
            moveToMyLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    @Suppress("MissingPermission")
    private fun moveToMyLocation() {
        if (!hasLocationPermission()) {
            return
        }

        val tokenSource = CancellationTokenSource()

        locationClient
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                tokenSource.token,
            ).addOnSuccessListener { location ->
                if (location == null) {
                    message("GPS location unavailable")
                    return@addOnSuccessListener
                }

                val point =
                    LatLng(
                        location.latitude,
                        location.longitude,
                    )

                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        point,
                        LOCATION_ZOOM,
                    ),
                )

                selectLocation(point)
            }.addOnFailureListener {
                message("Failed to get location")
            }
    }

    private fun showSaveDialog() {
        val point = selectedLocation ?: return
        val uid = userId ?: return

        val fields = createSaveFields()

        val dialog =
            AlertDialog
                .Builder(this)
                .setTitle("Save Hunting Spot")
                .setView(fields.layout)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create()

        dialog.setOnShowListener {
            dialog
                .getButton(
                    AlertDialog.BUTTON_POSITIVE,
                ).setOnClickListener {
                    val name =
                        fields.name.text
                            .toString()
                            .trim()

                    if (name.isBlank()) {
                        fields.name.error = "Enter a name"
                        return@setOnClickListener
                    }

                    val spot =
                        HuntingSpot(
                            userId = uid,
                            name = name,
                            latitude = point.latitude,
                            longitude = point.longitude,
                            notes =
                                fields.notes.text
                                    .toString()
                                    .trim(),
                        )

                    saveSpot(spot, dialog)
                }
        }

        dialog.show()
    }

    private fun createSaveFields(): SaveFields {
        val layout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 12, 48, 0)
            }

        val nameField =
            EditText(this).apply {
                hint = "Spot name"
                inputType = InputType.TYPE_CLASS_TEXT
            }

        val notesField =
            EditText(this).apply {
                hint = "Notes (optional)"
                inputType =
                    InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 3
            }

        layout.addView(nameField)
        layout.addView(notesField)

        return SaveFields(
            layout = layout,
            name = nameField,
            notes = notesField,
        )
    }

    // Handles both Room and validation failures.
    // Cancellation is rethrown instead of being treated as an error.
    @Suppress("TooGenericExceptionCaught")
    private fun saveSpot(
        spot: HuntingSpot,
        dialog: AlertDialog,
    ) {
        val button =
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        button.isEnabled = false

        lifecycleScope.launch {
            try {
                database.huntingSpotDao().insert(spot)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                button.isEnabled = true
                message(e.localizedMessage ?: "Save failed")
                return@launch
            }

            message("Hunting spot saved")
            dialog.dismiss()
            clearSelection()
        }
    }

    private fun clearSelection() {
        selectedLocation = null

        selectedMarker?.let {
            map.removeMarker(it)
        }

        selectedMarker = null
        selectedText.text = "No location selected"
        saveButton.isEnabled = false
    }

    private fun observeSpots() {
        val uid = userId ?: return

        lifecycleScope.launch {
            database
                .huntingSpotDao()
                .getSpotsByUser(uid)
                .collect { spots ->
                    savedMarkers.forEach {
                        map.removeMarker(it)
                    }

                    savedMarkers.clear()
                    markerSpots.clear()

                    spots.forEach { spot ->
                        val marker =
                            map.addMarker(
                                MarkerOptions()
                                    .position(
                                        LatLng(
                                            spot.latitude,
                                            spot.longitude,
                                        ),
                                    ).title(spot.name)
                                    .snippet(spot.notes),
                            )

                        savedMarkers.add(marker)
                        markerSpots[marker.id] = spot
                    }
                }
        }
    }

    private fun showSpotDetails(spot: HuntingSpot) {
        val uid = userId ?: return

        val details =
            """
            ${spot.notes}

            Latitude: ${spot.latitude}
            Longitude: ${spot.longitude}
            """.trimIndent()

        AlertDialog
            .Builder(this)
            .setTitle(spot.name)
            .setMessage(details)
            .setPositiveButton("Close", null)
            .setNegativeButton("Delete") { _, _ ->
                confirmDeleteSpot(spot, uid)
            }.show()
    }

    private fun confirmDeleteSpot(
        spot: HuntingSpot,
        uid: String,
    ) {
        AlertDialog
            .Builder(this)
            .setTitle("Delete Hunting Spot")
            .setMessage("Delete ${spot.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSpot(spot, uid)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    // Converts database failures to a visible error message.
    // Coroutine cancellation must propagate.
    @Suppress("TooGenericExceptionCaught")
    private fun deleteSpot(
        spot: HuntingSpot,
        uid: String,
    ) {
        lifecycleScope.launch {
            try {
                database
                    .huntingSpotDao()
                    .deleteById(spot.id, uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message(
                    e.localizedMessage ?: "Delete failed",
                )
                return@launch
            }

            message("Spot deleted")
        }
    }

    private fun message(text: String) {
        Toast
            .makeText(
                this,
                text,
                Toast.LENGTH_SHORT,
            ).show()
    }

    override fun onStart() {
        super.onStart()
        if (::mapView.isInitialized) {
            mapView.onStart()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) {
            mapView.onResume()
        }
    }

    override fun onPause() {
        if (::mapView.isInitialized) {
            mapView.onPause()
        }
        super.onPause()
    }

    override fun onStop() {
        if (::mapView.isInitialized) {
            mapView.onStop()
        }
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::mapView.isInitialized) {
            mapView.onLowMemory()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::mapView.isInitialized) {
            mapView.onSaveInstanceState(outState)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (::mapView.isInitialized) {
            mapView.onDestroy()
        }
        super.onDestroy()
    }
}
