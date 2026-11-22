
package com.huntersstory.app.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var repository: AuthRepository

    private val email = "hunter@example.com"
    private val password = "hunter123"

    @Before
    fun setup() {
        firebaseAuth = mockk()
        repository = AuthRepository(firebaseAuth)
    }

    private fun successfulAuthResult(): AuthResult {
        val user = mockk<FirebaseUser>()
        val result = mockk<AuthResult>()

        every {
            result.user
        } returns user

        return result
    }

    @Test
    fun currentUserReturnsFirebaseUser() {
        val user = mockk<FirebaseUser>()

        every {
            firebaseAuth.currentUser
        } returns user

        assertSame(
            user,
            repository.currentUser,
        )
    }

    @Test
    fun currentUserReturnsNullWhenLoggedOut() {
        every {
            firebaseAuth.currentUser
        } returns null

        assertNull(repository.currentUser)
    }

    @Test
    fun successfulLoginReturnsUser() =
        runBlocking {
            val result = successfulAuthResult()

            every {
                firebaseAuth.signInWithEmailAndPassword(
                    email,
                    password,
                )
            } returns Tasks.forResult(result)

            val user =
                repository.login(
                    email,
                    password,
                )

            assertNotNull(user)
            assertSame(result.user, user)

            verify(exactly = 1) {
                firebaseAuth.signInWithEmailAndPassword(
                    email,
                    password,
                )
            }
        }

    @Test
    fun loginTrimsEmail() =
        runBlocking {
            val result = successfulAuthResult()

            every {
                firebaseAuth.signInWithEmailAndPassword(
                    email,
                    password,
                )
            } returns Tasks.forResult(result)

            repository.login(
                "   $email   ",
                password,
            )

            verify(exactly = 1) {
                firebaseAuth.signInWithEmailAndPassword(
                    email,
                    password,
                )
            }
        }

    @Test
    fun loginKeepsPasswordUnchanged() =
        runBlocking {
            val passwordWithSpaces = " hunter123 "
            val result = successfulAuthResult()

            every {
                firebaseAuth.signInWithEmailAndPassword(
                    email,
                    passwordWithSpaces,
                )
            } returns Tasks.forResult(result)

            repository.login(
                email,
                passwordWithSpaces,
            )

            verify(exactly = 1) {
                firebaseAuth.signInWithEmailAndPassword(
                    email,
                    passwordWithSpaces,
                )
            }
        }

    @Test
    fun loginReturnsNullWhenResultHasNoUser() =
        runBlocking {
            val result = mockk<AuthResult>()

            every {
                result.user
            } returns null

            every {
                firebaseAuth.signInWithEmailAndPassword(
                    email,
                    password,
                )
            } returns Tasks.forResult(result)

            assertNull(
                repository.login(email, password),
            )
        }

    @Test
    fun loginPropagatesFirebaseFailure() =
        runBlocking {
            every {
                firebaseAuth.signInWithEmailAndPassword(
                    email,
                    password,
                )
            } returns
                Tasks.forException(
                    IllegalStateException("Invalid password"),
                )

            val error =
                runCatching {
                    repository.login(email, password)
                }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals(
                "Invalid password",
                error?.message,
            )
        }

    @Test
    fun successfulRegistrationReturnsUser() =
        runBlocking {
            val result = successfulAuthResult()

            every {
                firebaseAuth.createUserWithEmailAndPassword(
                    email,
                    password,
                )
            } returns Tasks.forResult(result)

            val user =
                repository.register(
                    email,
                    password,
                )

            assertNotNull(user)
            assertSame(result.user, user)

            verify(exactly = 1) {
                firebaseAuth.createUserWithEmailAndPassword(
                    email,
                    password,
                )
            }
        }

    @Test
    fun registrationTrimsEmail() =
        runBlocking {
            val result = successfulAuthResult()

            every {
                firebaseAuth.createUserWithEmailAndPassword(
                    email,
                    password,
                )
            } returns Tasks.forResult(result)

            repository.register(
                "   $email   ",
                password,
            )

            verify(exactly = 1) {
                firebaseAuth.createUserWithEmailAndPassword(
                    email,
                    password,
                )
            }
        }

    @Test
    fun registrationReturnsNullWhenUserMissing() =
        runBlocking {
            val result = mockk<AuthResult>()

            every {
                result.user
            } returns null

            every {
                firebaseAuth.createUserWithEmailAndPassword(
                    email,
                    password,
                )
            } returns Tasks.forResult(result)

            assertNull(
                repository.register(email, password),
            )
        }

    @Test
    fun registrationPropagatesFirebaseFailure() =
        runBlocking {
            every {
                firebaseAuth.createUserWithEmailAndPassword(
                    email,
                    password,
                )
            } returns
                Tasks.forException(
                    IllegalStateException("Email already used"),
                )

            val error =
                runCatching {
                    repository.register(email, password)
                }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals(
                "Email already used",
                error?.message,
            )
        }

    @Test
    fun logoutCallsFirebaseSignOut() {
        every {
            firebaseAuth.signOut()
        } returns Unit

        repository.logout()

        verify(exactly = 1) {
            firebaseAuth.signOut()
        }
    }
}
