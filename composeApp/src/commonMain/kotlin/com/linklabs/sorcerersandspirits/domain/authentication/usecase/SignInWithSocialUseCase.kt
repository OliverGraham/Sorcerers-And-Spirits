package com.linklabs.sorcerersandspirits.domain.authentication.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.authentication.LinkAccountResult
import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.SocialSignInProvider
import com.linklabs.sorcerersandspirits.domain.authentication.SocialTokenResult

/**
 * Authenticates *as* a provider account from the first-launch dialog: it recovers the matching
 * account if one already exists (e.g. after a reinstall) or provisions a new one. Unlike
 * [LinkAccountUseCase] (which upgrades the current anonymous account in place and treats a
 * collision as an error), there is no anonymous data to preserve here, so a pre-existing account is
 * the expected, successful case.
 */
class SignInWithSocialUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val socialSignInProvider: SocialSignInProvider,
) {
    suspend operator fun invoke(method: SignInMethod): LinkAccountResult =
        when (val token = socialSignInProvider.requestToken(method = method)) {
            is SocialTokenResult.Google ->
                authenticationRepository.signInWithGoogle(
                    idToken = token.idToken,
                    accessToken = token.accessToken,
                )

            is SocialTokenResult.Apple ->
                authenticationRepository.signInWithApple(
                    idToken = token.idToken,
                    rawNonce = token.rawNonce,
                )

            is SocialTokenResult.Cancelled -> LinkAccountResult.CANCELLED

            is SocialTokenResult.NotAvailable -> LinkAccountResult.NOT_AVAILABLE

            is SocialTokenResult.Error -> LinkAccountResult.ERROR
        }

    suspend fun withEmail(email: String, password: String): LinkAccountResult =
        authenticationRepository.signInWithEmail(email = email, password = password)
}
