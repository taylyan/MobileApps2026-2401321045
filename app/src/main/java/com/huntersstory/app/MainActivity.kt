
package com.huntersstory.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.huntersstory.app.ui.collection.CollectionActivity
import com.huntersstory.app.ui.gallery.GalleryActivity
import com.huntersstory.app.ui.journal.JournalActivity
import com.huntersstory.app.ui.login.LoginActivity
import com.huntersstory.app.ui.spots.SpotsActivity
import com.huntersstory.app.ui.sync.CloudSyncActivity

class MainActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            openLogin()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupWindowInsets()
        setupAccount()
        setupNavigation()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main),
        ) { view, insets ->
            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars(),
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom,
            )
            insets
        }
    }

    private fun setupAccount() {
        findViewById<TextView>(R.id.tvUserEmail).text =
            "Signed in as: ${auth.currentUser?.email}"

        findViewById<Button>(R.id.btnLogout)
            .setOnClickListener {
                auth.signOut()
                openLogin()
            }
    }

    private fun setupNavigation() {
        findViewById<Button>(R.id.btnJournal)
            .setOnClickListener {
                startActivity(
                    Intent(this, JournalActivity::class.java),
                )
            }

        findViewById<Button>(R.id.btnSpots)
            .setOnClickListener {
                startActivity(
                    Intent(this, SpotsActivity::class.java),
                )
            }

        findViewById<Button>(R.id.btnEquipment)
            .setOnClickListener {
                openCollection("equipment")
            }

        findViewById<Button>(R.id.btnDogs)
            .setOnClickListener {
                openCollection("dogs")
            }

        findViewById<Button>(R.id.btnGallery)
            .setOnClickListener {
                startActivity(
                    Intent(this, GalleryActivity::class.java),
                )
            }

        findViewById<Button>(R.id.btnCloudSync)
            .setOnClickListener {
                startActivity(
                    Intent(this, CloudSyncActivity::class.java),
                )
            }
    }

    private fun openCollection(collectionType: String) {
        val intent =
            Intent(this, CollectionActivity::class.java)

        intent.putExtra("collection_type", collectionType)
        startActivity(intent)
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}
