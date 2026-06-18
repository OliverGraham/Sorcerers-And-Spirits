package com.linklabs.sorcerersandspirits.data.authentication

import com.linklabs.sorcerersandspirits.domain.authentication.SignInMethod
import com.linklabs.sorcerersandspirits.domain.authentication.SocialSignInProvider
import com.linklabs.sorcerersandspirits.domain.authentication.SocialTokenResult

// TODO(SS-061): Implement the native pickers on iOS — GoogleSignIn SDK for Google (returning
//  SocialTokenResult.Google(idToken)) and AuthenticationServices (ASAuthorizationAppleIDProvider)
//  for Apple (returning SocialTokenResult.Apple(idToken, rawNonce) with a SHA256-hashed nonce).
//  Both require the GoogleSignIn pod / Apple capability and Firebase console setup, so until that
//  lands every request reports NotAvailable. Email/password linking does not route through this
//  provider and already works.
class IosSocialSignInProvider : SocialSignInProvider {
    override suspend fun requestToken(method: SignInMethod): SocialTokenResult =
        SocialTokenResult.NotAvailable
}
