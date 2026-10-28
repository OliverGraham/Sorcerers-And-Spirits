package com.linklabs.sorcerersandspirits.data.authentication

/**
 * Bridges the native iOS sign-in SDKs into shared code. Implemented in Swift (iosApp), where the
 * GoogleSignIn SDK and AuthenticationServices / CryptoKit live, and injected into Koin from
 * `initializeKoin`. Each method presents the native account picker and invokes its completion
 * exactly once.
 *
 * Results are passed back as objects (not multi-arg closures) so the Swift side stays clean — a
 * Kotlin `Boolean` in a *function-type* parameter would surface in Swift as a boxed
 * `KotlinBoolean`, whereas a constructor parameter bridges to a plain Swift `Bool`.
 */
interface IosSocialSignInBridge {
    fun signInWithGoogle(onResult: (IosGoogleSignInResult) -> Unit)

    fun signInWithApple(onResult: (IosAppleSignInResult) -> Unit)
}

/**
 * - `idToken == null && !isCancelled` signals an error.
 * - `accessToken` is required to build the Firebase Google credential on iOS.
 */
data class IosGoogleSignInResult(
    val idToken: String?,
    val accessToken: String?,
    val isCancelled: Boolean,
)

/**
 * - `idToken == null && !isCancelled` signals an error.
 * - `rawNonce` is the nonce Swift generated; its SHA-256 hash was sent to Apple, and Firebase needs
 *   the raw value to verify the returned identity token.
 */
data class IosAppleSignInResult(
    val idToken: String?,
    val rawNonce: String?,
    val authorizationCode: String?,
    val isCancelled: Boolean,
)
