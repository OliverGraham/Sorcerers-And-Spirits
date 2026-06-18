package com.linklabs.sorcerersandspirits.data.authentication

import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.SocialSignInProvider
import com.linklabs.sorcerersandspirits.domain.authentication.SocialTokenResult

// TODO(SS-061): Implement the native Google account picker via androidx.credentials
//  (CredentialManager + GetSignInWithGoogleOption using the Firebase web client ID), returning
//  SocialTokenResult.Google(idToken). Apple sign-in on Android needs the OAuth web flow. Both also
//  require Firebase console setup (OAuth client IDs + SHA fingerprints) before they can succeed, so
//  until that lands every request reports NotAvailable and the LinkAccountUseCase surfaces it as
//  NOT_AVAILABLE. Email/password linking does not route through this provider and already works.
class AndroidSocialSignInProvider : SocialSignInProvider {
    override suspend fun requestToken(method: SignInMethod): SocialTokenResult =
        SocialTokenResult.NotAvailable
}
