
package com.huntersstory.app.ui.journal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.huntersstory.app.R

class JournalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal)

        val addHuntButton = findViewById<Button>(R.id.btnAddHunt)

        addHuntButton.setOnClickListener {
            val intent = Intent(this, AddHuntActivity::class.java)
            startActivity(intent)
        }
    }
}
