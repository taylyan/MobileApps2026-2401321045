package com.huntersstory.app.ui.journal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import com.huntersstory.app.ui.journal.viewmodel.HuntingTripViewModel
import kotlinx.coroutines.launch

class JournalActivity : AppCompatActivity() {
    private lateinit var viewModel: HuntingTripViewModel
    private lateinit var adapter: HuntingTripAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal)

        findViewById<Button>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        viewModel =
            ViewModelProvider(this)[HuntingTripViewModel::class.java]

        val addHuntButton = findViewById<Button>(R.id.btnAddHunt)
        val recyclerView =
            findViewById<RecyclerView>(R.id.rvHuntingTrips)
        val emptyMessage =
            findViewById<TextView>(R.id.tvEmptyJournal)

        adapter =
            HuntingTripAdapter(
                onDeleteClick = { trip ->
                    AlertDialog
                        .Builder(this)
                        .setTitle("Delete Hunting Trip")
                        .setMessage(
                            "Are you sure you want to delete this hunting trip?",
                        ).setPositiveButton("Delete") { _, _ ->
                            viewModel.deleteTrip(trip)
                        }.setNegativeButton("Cancel", null)
                        .show()
                },
                onEditClick = { trip ->
                    val intent = Intent(this, AddHuntActivity::class.java)
                    intent.putExtra("trip_id", trip.id)
                    startActivity(intent)
                },
            )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        addHuntButton.setOnClickListener {
            startActivity(Intent(this, AddHuntActivity::class.java))
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Toast
                .makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_SHORT,
                ).show()
            finish()
            return
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getTripsByUser(userId).collect { trips ->
                    adapter.submitList(trips)
                    emptyMessage.visibility =
                        if (trips.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}
