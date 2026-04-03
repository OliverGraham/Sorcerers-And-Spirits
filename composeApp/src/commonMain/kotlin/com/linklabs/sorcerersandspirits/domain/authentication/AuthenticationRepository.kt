package com.linklabs.sorcerersandspirits.domain.authentication

interface AuthenticationRepository {
    fun getUserId(): String?

    suspend fun isSignedIn(): Boolean

    suspend fun signInAnonymously(): String?
}
