package com.linklabs.sorcerersandspirits.data.authentication

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SessionStore
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.first

private const val APPLE_PROVIDER_ID = "apple.com"

private fun googleCredential(idToken: String, accessToken: String?): AuthCredential = GoogleAuthProvider.credential(idToken = idToken, accessToken = accessToken)

private fun appleCredential(idToken: String, rawNonce: String): AuthCredential = OAuthProvider.credential(providerId = APPLE_PROVIDER_ID, idToken = idToken, rawNonce = rawNonce)

class FirebaseAuthenticationRepository(private val sessionStore: SessionStore) : AuthenticationRepository {
    private val auth: FirebaseAuth = Firebase.auth

    override fun getUserId() = auth.currentUser?.uid ?: sessionStore.cached()?.userId

    override suspend fun isSignedIn(): Boolean {
        val localSession = sessionStore.restore()
        val firebaseUser = auth.authStateChanged.first()
        return firebaseUser != null || localSession != null
    }

    override suspend fun signInAnonymously() = auth.signInAnonymously().user?.uid

    override suspend fun linkWithGoogle(idToken: String, accessToken: String?): LinkAccountResult = link(credential = googleCredential(idToken = idToken, accessToken = accessToken))

    override suspend fun linkWithApple(idToken: String, rawNonce: String): LinkAccountResult = link(credential = appleCredential(idToken = idToken, rawNonce = rawNonce))

    override suspend fun linkWithEmail(email: String, password: String): LinkAccountResult = link(credential = EmailAuthProvider.credential(email = email, password = password))

    override suspend fun signInWithGoogle(
        idToken: String,
        accessToken: String?,
    ): LinkAccountResult = signIn(credential = googleCredential(idToken = idToken, accessToken = accessToken))

    override suspend fun signInWithApple(idToken: String, rawNonce: String): LinkAccountResult = signIn(credential = appleCredential(idToken = idToken, rawNonce = rawNonce))

    /**
     * Creates the account first; a collision means the email is already registered, so recover it
     * by signing in. This ordering also sidesteps Firebase's email-enumeration protection, which
     * would otherwise mask "no such account" on a sign-in attempt as a generic invalid-credentials
     * error.
     */
    override suspend fun signInWithEmail(email: String, password: String): LinkAccountResult = try {
        auth.createUserWithEmailAndPassword(email = email, password = password)
        LinkAccountResult.SUCCESS
    } catch (_: FirebaseAuthUserCollisionException) {
        signInWithEmailAndPassword(email = email, password = password)
    } catch (_: FirebaseAuthWeakPasswordException) {
        LinkAccountResult.INVALID_CREDENTIAL
    } catch (_: FirebaseAuthInvalidCredentialsException) {
        LinkAccountResult.INVALID_CREDENTIAL
    } catch (_: Exception) {
        LinkAccountResult.ERROR
    }

    private suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): LinkAccountResult = try {
        auth.signInWithEmailAndPassword(email = email, password = password)
        LinkAccountResult.SUCCESS
    } catch (_: FirebaseAuthInvalidUserException) {
        LinkAccountResult.INVALID_CREDENTIAL
    } catch (_: FirebaseAuthInvalidCredentialsException) {
        // The email exists but the password is wrong.
        LinkAccountResult.INVALID_CREDENTIAL
    } catch (_: Exception) {
        LinkAccountResult.ERROR
    }

    /**
     * Links a credential to the CURRENT account in place, so its userId — and the profile/stats
     * already keyed to it — carries over to the permanent identity. A collision means the
     * credential already belongs to a different account, which is an error here (use [signIn] to
     * authenticate as that account instead).
     */
    private suspend fun link(credential: AuthCredential): LinkAccountResult {
        val currentUser = auth.currentUser ?: return LinkAccountResult.NO_CURRENT_USER

        return try {
            currentUser.linkWithCredential(credential = credential)
            LinkAccountResult.SUCCESS
        } catch (_: FirebaseAuthUserCollisionException) {
            LinkAccountResult.CREDENTIAL_IN_USE
        } catch (_: FirebaseAuthWeakPasswordException) {
            LinkAccountResult.INVALID_CREDENTIAL
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            LinkAccountResult.INVALID_CREDENTIAL
        } catch (_: Exception) {
            LinkAccountResult.ERROR
        }
    }

    /**
     * Authenticates as the credential's account: Firebase recovers the matching account if one
     * exists, otherwise provisions a new one. Either way the previous (anonymous) session is left
     * behind — used only from the first-launch dialog, where there is no local data to preserve.
     */
    private suspend fun signIn(credential: AuthCredential): LinkAccountResult = try {
        auth.signInWithCredential(authCredential = credential)
        LinkAccountResult.SUCCESS
    } catch (_: FirebaseAuthInvalidCredentialsException) {
        LinkAccountResult.INVALID_CREDENTIAL
    } catch (_: Exception) {
        LinkAccountResult.ERROR
    }
}
