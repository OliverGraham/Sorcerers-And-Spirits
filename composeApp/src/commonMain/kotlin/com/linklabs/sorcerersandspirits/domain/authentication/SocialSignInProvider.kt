package com.linklabs.sorcerersandspirits.domain.authentication

/**
 * Obtains a federated identity token from a native account picker (Google, Apple). This is the one
 * piece of the linking flow that can't be shared: the gitlive SDK can consume the resulting
 * credential cross-platform, but acquiring it requires platform UI (Credential Manager on Android,
 * the GoogleSignIn / AuthenticationServices SDKs on iOS). Each platform supplies its own
 * implementation via DI.
 */
interface SocialSignInProvider {
    suspend fun requestToken(method: SignInMethod): SocialTokenResult
}

/** A federated token harvested from a provider's account picker, ready to hand to Firebase. */
sealed interface SocialTokenResult {
    // accessToken is required to build the Google credential on iOS (credentialWithIDToken:
    // accessToken:); Android's Credential Manager ID-token flow doesn't surface one, so it passes
    // null there.
    data class Google(val idToken: String, val accessToken: String?) : SocialTokenResult

    data class Apple(
        val idToken: String,
        val rawNonce: String,
        val authorizationCode: String?,
    ) : SocialTokenResult

    data object Cancelled : SocialTokenResult

    /** The provider isn't wired up on this platform yet. See SS-061 follow-ups. */
    data object NotAvailable : SocialTokenResult

    data object Error : SocialTokenResult
}
