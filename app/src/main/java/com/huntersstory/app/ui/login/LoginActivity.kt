package com.huntersstory.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.huntersstory.app.MainActivity
import com.huntersstory.app.R
import com.huntersstory.app.ui.auth.AuthUiState
import com.huntersstory.app.ui.auth.AuthViewModel
import com.huntersstory.app.ui.register.RegisterActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerText = findViewById<TextView>(R.id.tvRegister)

        loginButton.setOnClickListener {
            viewModel.login(
                email.text.toString(),
                password.text.toString(),
            )
        }

        registerText.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java),
            )
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    AuthUiState.Idle -> {
                        loginButton.isEnabled = true
                    }

                    AuthUiState.Loading -> {
                        loginButton.isEnabled = false
                    }

                    AuthUiState.Success -> {
                        val intent =
                            Intent(
                                this@LoginActivity,
                                MainActivity::class.java,
                            )
                        startActivity(intent)
                        finish()
                    }

                    is AuthUiState.Error -> {
                        loginButton.isEnabled = true
                        Toast
                            .makeText(
                                this@LoginActivity,
                                state.message,
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (FirebaseAuth.getInstance().currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
