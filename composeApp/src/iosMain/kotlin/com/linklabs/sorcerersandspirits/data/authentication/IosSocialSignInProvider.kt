package com.linklabs.sorcerersandspirits.data.authentication

import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.SocialSignInProvider
import com.linklabs.sorcerersandspirits.domain.authentication.SocialTokenResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class IosSocialSignInProvider(private val bridge: IosSocialSignInBridge) : SocialSignInProvider {

    override suspend fun requestToken(method: SignInMethod): SocialTokenResult = when (method) {
        SignInMethod.GOOGLE -> requestGoogleToken()

        SignInMethod.APPLE -> requestAppleToken()

        // Email never routes through this provider; it is linked directly by the repository.
        SignInMethod.EMAIL -> SocialTokenResult.NotAvailable
    }

    private suspend fun requestGoogleToken(): SocialTokenResult =
        suspendCancellableCoroutine { continuation ->
            bridge.signInWithGoogle { googleResult ->
                val result =
                    when {
                        googleResult.isCancelled -> SocialTokenResult.Cancelled

                        googleResult.idToken == null -> SocialTokenResult.Error

                        else ->
                            SocialTokenResult.Google(
                                idToken = googleResult.idToken,
                                accessToken = googleResult.accessToken,
                            )
                    }
                continuation.resume(value = result)
            }
        }

    private suspend fun requestAppleToken(): SocialTokenResult =
        suspendCancellableCoroutine { continuation ->
            bridge.signInWithApple { appleResult ->
                val result =
                    when {
                        appleResult.isCancelled -> SocialTokenResult.Cancelled

                        appleResult.idToken == null || appleResult.rawNonce == null ->
                            SocialTokenResult.Error

                        else ->
                            SocialTokenResult.Apple(
                                idToken = appleResult.idToken,
                                rawNonce = appleResult.rawNonce,
                            )
                    }
                continuation.resume(value = result)
            }
        }
}
