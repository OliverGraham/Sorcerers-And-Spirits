package com.linklabs.sorcerersandspirits.domain.authentication.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.SocialSignInProvider
import com.linklabs.sorcerersandspirits.domain.authentication.SocialTokenResult

/**
 * Upgrades the current (anonymous) account into a permanent one by linking a [SignInMethod],
 * preserving the existing userId so the profile and stats already tied to it carry over.
 *
 * Reusable from any caller that owns the link affordance — the first-launch dialog and the
 * ProfileScreen both drive this single use case.
 */
class LinkAccountUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val socialSignInProvider: SocialSignInProvider,
) {
    /** Links a provider that uses a native account picker (Google or Apple). */
    suspend operator fun invoke(method: SignInMethod): LinkAccountResult =
        when (val token = socialSignInProvider.requestToken(method = method)) {
            is SocialTokenResult.Google ->
                authenticationRepository.linkWithGoogle(idToken = token.idToken)

            is SocialTokenResult.Apple ->
                authenticationRepository.linkWithApple(
                    idToken = token.idToken,
                    rawNonce = token.rawNonce,
                )

            is SocialTokenResult.Cancelled -> LinkAccountResult.CANCELLED

            is SocialTokenResult.NotAvailable -> LinkAccountResult.NOT_AVAILABLE

            is SocialTokenResult.Error -> LinkAccountResult.ERROR
        }

    /** Links an email/password identity collected in-app (no native picker involved). */
    suspend fun withEmail(email: String, password: String): LinkAccountResult =
        authenticationRepository.linkWithEmail(email = email, password = password)
}
