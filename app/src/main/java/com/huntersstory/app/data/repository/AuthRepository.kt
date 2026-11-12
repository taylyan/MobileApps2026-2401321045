
package com.huntersstory.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun register(
        email: String,
        password: String
    ): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(
            email.trim(),
            password
        ).await()

        return result.user
    }

    suspend fun login(
        email: String,
        password: String
    ): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(
            email.trim(),
            password
        ).await()

        return result.user
    }

    fun logout() {
        auth.signOut()
    }
}
