package com.linklabs.sorcerersandspirits.data

import com.google.firebase.auth.FirebaseAuth
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import kotlinx.coroutines.tasks.await

class AndroidAuthenticationRepository : AuthenticationRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun getUserId() = auth.currentUser?.uid

    override suspend fun isSignedIn() = auth.currentUser != null

    override suspend fun signInAnonymously() {
        auth.signInAnonymously().await()
    }
}
