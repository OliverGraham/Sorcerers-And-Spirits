package com.linklabs.sorcerersandspirits.domain.authentication

interface AuthenticationRepository {
    fun getUserId(): String?

    suspend fun isSignedIn(): Boolean

    fun isAnonymous(): Boolean

    suspend fun signInAnonymously(): String?

    suspend fun signOut()

    // link* attaches a provider to the CURRENT account (preserving its userId/stats); a collision
    // with an existing account is an error. Used when upgrading an anonymous account in place.
    suspend fun linkWithGoogle(idToken: String, accessToken: String?): LinkAccountResult

    suspend fun linkWithApple(idToken: String, rawNonce: String): LinkAccountResult

    suspend fun linkWithEmail(email: String, password: String): LinkAccountResult

    // signIn* authenticates AS the provider account: it recovers an existing account if one matches
    // the credential, otherwise creates a fresh one. Used from the first-launch dialog, where there
    // is no anonymous data to preserve.
    suspend fun signInWithGoogle(idToken: String, accessToken: String?): LinkAccountResult

    suspend fun signInWithApple(idToken: String, rawNonce: String): LinkAccountResult

    suspend fun signInWithEmail(email: String, password: String): LinkAccountResult
}
