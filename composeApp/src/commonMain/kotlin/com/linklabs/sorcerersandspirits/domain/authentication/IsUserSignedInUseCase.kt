package com.linklabs.sorcerersandspirits.domain.authentication

class IsUserSignedInUseCase(private val authenticationRepository: AuthenticationRepository) {
    suspend operator fun invoke() = authenticationRepository.isSignedIn()
}
