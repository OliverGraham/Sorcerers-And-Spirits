package com.linklabs.sorcerersandspirits.domain.authentication

class SignInAnonymouslyUseCase(
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke() {
        if (!authenticationRepository.isSignedIn()) {
            authenticationRepository.signInAnonymously()
        }
    }
}
