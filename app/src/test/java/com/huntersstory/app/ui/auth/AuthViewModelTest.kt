package com.huntersstory.app.ui.auth

import com.google.firebase.auth.FirebaseUser
import com.huntersstory.app.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    private val email = "hunter@example.com"
    private val password = "hunter123"

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())

        repository = mockk(relaxed = true)
        viewModel = AuthViewModel(repository)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private fun assertError(expected: String) {
        val state = viewModel.uiState.value

        assertTrue(state is AuthUiState.Error)
        assertEquals(
            expected,
            (state as AuthUiState.Error).message,
        )
    }

    // ==================================
    // INITIAL STATE
    // ==================================

    @Test
    fun initialStateIsIdle() {
        assertEquals(
            AuthUiState.Idle,
            viewModel.uiState.value,
        )
    }

    @Test
    fun clearStateKeepsIdleState() {
        viewModel.clearState()

        assertEquals(
            AuthUiState.Idle,
            viewModel.uiState.value,
        )
    }

    // ==================================
    // INPUT VALIDATION
    // ==================================

    @Test
    fun loginWithEmptyEmailShowsError() {
        viewModel.login("", password)

        assertError(
            "Email and password are required.",
        )

        coVerify(exactly = 0) {
            repository.login(any(), any())
        }
    }

    @Test
    fun loginWithEmptyPasswordShowsError() {
        viewModel.login(email, "")

        assertError(
            "Email and password are required.",
        )
    }

    @Test
    fun loginWithBlankEmailShowsError() {
        viewModel.login("   ", password)

        assertError(
            "Email and password are required.",
        )
    }

    @Test
    fun loginWithBlankPasswordShowsError() {
        viewModel.login(email, "   ")

        assertError(
            "Email and password are required.",
        )
    }

    @Test
    fun registerWithEmptyEmailShowsError() {
        viewModel.register("", password)

        assertError(
            "Email and password are required.",
        )
    }

    @Test
    fun registerWithEmptyPasswordShowsError() {
        viewModel.register(email, "")

        assertError(
            "Email and password are required.",
        )
    }

    @Test
    fun registerWithShortPasswordShowsError() {
        viewModel.register(email, "12345")

        assertError(
            "Password must be at least 6 characters.",
        )

        coVerify(exactly = 0) {
            repository.register(any(), any())
        }
    }

    @Test
    fun registerWithSixCharacterPasswordIsAllowed() =
        runTest {
            val user = mockk<FirebaseUser>()

            coEvery {
                repository.register(email, "123456")
            } returns user

            viewModel.register(email, "123456")

            advanceUntilIdle()

            assertEquals(
                AuthUiState.Success,
                viewModel.uiState.value,
            )
        }

    // ==================================
    // LOGIN
    // ==================================

    @Test
    fun successfulLoginSetsSuccessState() =
        runTest {
            val user = mockk<FirebaseUser>()

            coEvery {
                repository.login(email, password)
            } returns user

            viewModel.login(email, password)

            advanceUntilIdle()

            assertEquals(
                AuthUiState.Success,
                viewModel.uiState.value,
            )

            coVerify(exactly = 1) {
                repository.login(email, password)
            }
        }

    @Test
    fun loginWithNullUserShowsError() =
        runTest {
            coEvery {
                repository.login(email, password)
            } returns null

            viewModel.login(email, password)

            advanceUntilIdle()

            assertError("Authentication failed.")
        }

    @Test
    fun loginExceptionShowsError() =
        runTest {
            coEvery {
                repository.login(email, password)
            } throws
                IllegalStateException(
                    "Invalid credentials",
                )

            viewModel.login(email, password)

            advanceUntilIdle()

            assertError("Invalid credentials")
        }

    @Test
    fun loginExceptionWithoutMessageUsesFallback() =
        runTest {
            coEvery {
                repository.login(email, password)
            } throws Exception()

            viewModel.login(email, password)

            advanceUntilIdle()

            assertError(
                "An unexpected error occurred.",
            )
        }

    @Test
    fun loginFirstEntersLoadingState() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val continueLogin = CompletableDeferred<Unit>()
            val user = mockk<FirebaseUser>()

            coEvery {
                repository.login(email, password)
            } coAnswers {
                started.complete(Unit)
                continueLogin.await()
                user
            }

            viewModel.login(email, password)

            runCurrent()
            started.await()

            assertEquals(
                AuthUiState.Loading,
                viewModel.uiState.value,
            )

            continueLogin.complete(Unit)
            advanceUntilIdle()

            assertEquals(
                AuthUiState.Success,
                viewModel.uiState.value,
            )
        }

    @Test
    fun secondLoginWhileLoadingIsIgnored() =
        runTest {
            val continueLogin = CompletableDeferred<Unit>()
            val user = mockk<FirebaseUser>()

            coEvery {
                repository.login(email, password)
            } coAnswers {
                continueLogin.await()
                user
            }

            viewModel.login(email, password)
            runCurrent()

            assertEquals(
                AuthUiState.Loading,
                viewModel.uiState.value,
            )

            viewModel.login(email, password)
            runCurrent()

            coVerify(exactly = 1) {
                repository.login(email, password)
            }

            continueLogin.complete(Unit)
            advanceUntilIdle()
        }

    // ==================================
    // REGISTER
    // ==================================

    @Test
    fun successfulRegistrationSetsSuccessState() =
        runTest {
            val user = mockk<FirebaseUser>()

            coEvery {
                repository.register(email, password)
            } returns user

            viewModel.register(email, password)

            advanceUntilIdle()

            assertEquals(
                AuthUiState.Success,
                viewModel.uiState.value,
            )

            coVerify(exactly = 1) {
                repository.register(email, password)
            }
        }

    @Test
    fun registerWithNullUserShowsError() =
        runTest {
            coEvery {
                repository.register(email, password)
            } returns null

            viewModel.register(email, password)

            advanceUntilIdle()

            assertError("Authentication failed.")
        }

    @Test
    fun registerExceptionShowsError() =
        runTest {
            coEvery {
                repository.register(email, password)
            } throws
                IllegalStateException(
                    "Email already in use",
                )

            viewModel.register(email, password)

            advanceUntilIdle()

            assertError("Email already in use")
        }

    @Test
    fun registerFirstEntersLoadingState() =
        runTest {
            val continueRegister = CompletableDeferred<Unit>()
            val user = mockk<FirebaseUser>()

            coEvery {
                repository.register(email, password)
            } coAnswers {
                continueRegister.await()
                user
            }

            viewModel.register(email, password)
            runCurrent()

            assertEquals(
                AuthUiState.Loading,
                viewModel.uiState.value,
            )

            continueRegister.complete(Unit)
            advanceUntilIdle()

            assertEquals(
                AuthUiState.Success,
                viewModel.uiState.value,
            )
        }

    // ==================================
    // LOGOUT AND CLEAR STATE
    // ==================================

    @Test
    fun logoutCallsRepository() {
        viewModel.logout()

        verify(exactly = 1) {
            repository.logout()
        }
    }

    @Test
    fun logoutResetsErrorState() {
        viewModel.login("", "")

        assertTrue(
            viewModel.uiState.value is AuthUiState.Error,
        )

        viewModel.logout()

        assertEquals(
            AuthUiState.Idle,
            viewModel.uiState.value,
        )
    }

    @Test
    fun clearStateResetsError() {
        viewModel.register(email, "123")

        assertTrue(
            viewModel.uiState.value is AuthUiState.Error,
        )

        viewModel.clearState()

        assertEquals(
            AuthUiState.Idle,
            viewModel.uiState.value,
        )
    }

    @Test
    fun validLoginAllowsShortPassword() =
        runTest {
            val user = mockk<FirebaseUser>()

            coEvery {
                repository.login(email, "123")
            } returns user

            viewModel.login(email, "123")

            advanceUntilIdle()

            assertEquals(
                AuthUiState.Success,
                viewModel.uiState.value,
            )
        }
}
