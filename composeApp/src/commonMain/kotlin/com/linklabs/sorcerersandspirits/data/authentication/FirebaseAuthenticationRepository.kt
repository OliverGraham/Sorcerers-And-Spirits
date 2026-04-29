package com.linklabs.sorcerersandspirits.data.authentication

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth

class FirebaseAuthenticationRepository : AuthenticationRepository {
    private val auth: FirebaseAuth = Firebase.auth

    override fun getUserId() = auth.currentUser?.uid

    override suspend fun isSignedIn() = auth.currentUser != null

    override suspend fun signInAnonymously() = auth.signInAnonymously().user?.uid
}
