package com.linklabs.sorcerersandspirits.domain.authentication

interface AuthenticationRepository {
    fun getUserId(): String?

    suspend fun isSignedIn(): Boolean

    suspend fun signInAnonymously(): String?

    suspend fun linkWithGoogle(idToken: String): LinkAccountResult

    suspend fun linkWithApple(idToken: String, rawNonce: String): LinkAccountResult

    suspend fun linkWithEmail(email: String, password: String): LinkAccountResult
}
