package com.huntersstory.app.ui.register

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.huntersstory.app.R
import com.huntersstory.app.ui.auth.AuthUiState
import com.huntersstory.app.ui.auth.AuthViewModel
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val email = findViewById<EditText>(R.id.etRegisterEmail)
        val password = findViewById<EditText>(R.id.etRegisterPassword)
        val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val backToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        registerButton.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString()
            val confirmText = confirmPassword.text.toString()

            if (passwordText != confirmText) {
                confirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            viewModel.register(emailText, passwordText)
        }

        backToLogin.setOnClickListener {
            finish()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        AuthUiState.Idle -> {
                            registerButton.isEnabled = true
                        }

                        AuthUiState.Loading -> {
                            registerButton.isEnabled = false
                        }

                        AuthUiState.Success -> {
                            Toast.makeText(
                                this@RegisterActivity,
                                "Account created successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }

                        is AuthUiState.Error -> {
                            registerButton.isEnabled = true
                            Toast.makeText(
                                this@RegisterActivity,
                                state.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
