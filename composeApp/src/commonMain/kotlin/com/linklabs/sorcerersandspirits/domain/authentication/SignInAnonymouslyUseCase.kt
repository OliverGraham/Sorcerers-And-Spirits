package com.linklabs.sorcerersandspirits.domain.authentication

// TODO: Delete? Why not used?
class SignInAnonymouslyUseCase(private val authenticationRepository: AuthenticationRepository) {
    suspend operator fun invoke() {
        if (!authenticationRepository.isSignedIn()) {
            authenticationRepository.signInAnonymously()
        }
    }
}
