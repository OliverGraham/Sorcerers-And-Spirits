package com.linklabs.sorcerersandspirits.data.authentication

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth

private const val APPLE_PROVIDER_ID = "apple.com"

class FirebaseAuthenticationRepository : AuthenticationRepository {
    private val auth: FirebaseAuth = Firebase.auth

    override fun getUserId() = auth.currentUser?.uid

    override suspend fun isSignedIn() = auth.currentUser != null

    override suspend fun signInAnonymously() = auth.signInAnonymously().user?.uid

    override suspend fun linkWithGoogle(idToken: String): LinkAccountResult =
        link(credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null))

    override suspend fun linkWithApple(idToken: String, rawNonce: String): LinkAccountResult =
        link(
            credential =
                OAuthProvider.credential(
                    providerId = APPLE_PROVIDER_ID,
                    idToken = idToken,
                    rawNonce = rawNonce,
                )
        )

    override suspend fun linkWithEmail(email: String, password: String): LinkAccountResult =
        link(credential = EmailAuthProvider.credential(email = email, password = password))

    // Linking (rather than a fresh sign-in) upgrades the existing anonymous account in place, so
    // the
    // userId — and the profile/stats already keyed to it — carries over to the permanent identity.
    private suspend fun link(credential: AuthCredential): LinkAccountResult {
        val currentUser = auth.currentUser ?: return LinkAccountResult.NO_CURRENT_USER

        return try {
            currentUser.linkWithCredential(credential = credential)
            LinkAccountResult.SUCCESS
        } catch (collision: FirebaseAuthUserCollisionException) {
            LinkAccountResult.CREDENTIAL_IN_USE
        } catch (weakPassword: FirebaseAuthWeakPasswordException) {
            LinkAccountResult.INVALID_CREDENTIAL
        } catch (invalidCredentials: FirebaseAuthInvalidCredentialsException) {
            LinkAccountResult.INVALID_CREDENTIAL
        } catch (error: Exception) {
            LinkAccountResult.ERROR
        }
    }
}
