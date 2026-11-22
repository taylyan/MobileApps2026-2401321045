
package com.huntersstory.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huntersstory.app.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {
    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }

    private val _uiState =
        MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    fun register(
        email: String,
        password: String,
    ) {
        authenticate(email, password, true)
    }

    fun login(
        email: String,
        password: String,
    ) {
        authenticate(email, password, false)
    }

    // Authentication errors are converted into UI states.
    // Coroutine cancellation must propagate normally.
    @Suppress("TooGenericExceptionCaught")
    private fun authenticate(
        email: String,
        password: String,
        isRegister: Boolean,
    ) {
        if (_uiState.value == AuthUiState.Loading) {
            return
        }

        val validationError =
            when {
                email.isBlank() || password.isBlank() ->
                    "Email and password are required."

                isRegister && password.length < MIN_PASSWORD_LENGTH ->
                    "Password must be at least 6 characters."

                else -> null
            }

        if (validationError != null) {
            _uiState.value =
                AuthUiState.Error(
                    validationError,
                )
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            try {
                val user =
                    if (isRegister) {
                        repository.register(
                            email,
                            password,
                        )
                    } else {
                        repository.login(
                            email,
                            password,
                        )
                    }

                _uiState.value =
                    if (user != null) {
                        AuthUiState.Success
                    } else {
                        AuthUiState.Error(
                            "Authentication failed.",
                        )
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value =
                    AuthUiState.Error(
                        e.localizedMessage
                            ?: "An unexpected error occurred.",
                    )
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState.Idle
    }

    fun clearState() {
        _uiState.value = AuthUiState.Idle
    }
}
