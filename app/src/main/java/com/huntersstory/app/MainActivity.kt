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
import com.huntersstory.app.ui.login.LoginActivity
import com.huntersstory.app.ui.journal.JournalActivity
import com.huntersstory.app.ui.spots.SpotsActivity
import com.huntersstory.app.ui.collection.CollectionActivity
import com.huntersstory.app.ui.gallery.GalleryActivity
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

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        val userEmail = findViewById<TextView>(R.id.tvUserEmail)
        val logoutButton = findViewById<Button>(R.id.btnLogout)

        userEmail.text = "Signed in as: ${auth.currentUser?.email}"

        logoutButton.setOnClickListener {
            auth.signOut()
            openLogin()
        }

        val journalButton = findViewById<Button>(R.id.btnJournal)

        journalButton.setOnClickListener {
            val intent = Intent(this, JournalActivity::class.java)
            startActivity(intent)
        }

        val spotsButton = findViewById<Button>(R.id.btnSpots)

        spotsButton.setOnClickListener {
            startActivity(
                Intent(this, SpotsActivity::class.java)
            )
        }

        val equipmentButton =
            findViewById<Button>(R.id.btnEquipment)

        equipmentButton.setOnClickListener {
            val intent = Intent(
                this,
                CollectionActivity::class.java
            )

            intent.putExtra("collection_type", "equipment")
            startActivity(intent)
        }

        val dogsButton =
            findViewById<Button>(R.id.btnDogs)

        dogsButton.setOnClickListener {
            val intent = Intent(
                this,
                CollectionActivity::class.java
            )

            intent.putExtra("collection_type", "dogs")
            startActivity(intent)
        }

        val galleryButton = findViewById<Button>(R.id.btnGallery)

        galleryButton.setOnClickListener {
            startActivity(
                Intent(this, GalleryActivity::class.java)
            )
        }

    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
