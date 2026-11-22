
package com.huntersstory.app.ui.sync

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.huntersstory.app.R
import kotlinx.coroutines.launch

class CloudSyncActivity : AppCompatActivity() {

    private lateinit var viewModel: CloudSyncViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_cloud_sync)

        viewModel = ViewModelProvider(this)[
            CloudSyncViewModel::class.java
        ]

        val uploadButton = findViewById<Button>(
            R.id.btnUploadBackup
        )

        val restoreButton = findViewById<Button>(
            R.id.btnRestoreBackup
        )

        val progress = findViewById<ProgressBar>(
            R.id.progressSync
        )

        val status = findViewById<TextView>(
            R.id.tvSyncStatus
        )

        uploadButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Upload Cloud Backup")
                .setMessage(
                    "This will replace your previous cloud backup. Continue?"
                )
                .setPositiveButton("Upload") { _, _ ->
                    viewModel.upload(uid)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        restoreButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Restore Cloud Backup")
                .setMessage(
                    "This will replace your local hunting trips, spots, equipment and dogs for this account with the cloud backup. Local-only changes not in the backup will be lost. Continue?"
                )
                .setPositiveButton("Restore") { _, _ ->
                    viewModel.restore(uid)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->

                    val loading = state is SyncState.Loading

                    uploadButton.isEnabled = !loading
                    restoreButton.isEnabled = !loading

                    progress.visibility =
                        if (loading) View.VISIBLE else View.GONE

                    status.text = when (state) {
                        SyncState.Idle -> "Ready"
                        SyncState.Loading ->
                            "Processing cloud backup..."
                        is SyncState.Success -> state.message
                        is SyncState.Error -> state.message
                    }
                }
            }
        }
    }
}
