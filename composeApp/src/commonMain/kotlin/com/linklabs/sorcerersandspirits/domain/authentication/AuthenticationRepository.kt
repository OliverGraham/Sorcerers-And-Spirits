package com.linklabs.sorcerersandspirits.domain.authentication

interface AuthenticationRepository {
    suspend fun isSignedIn(): Boolean
    suspend fun signInAnonymously()
}