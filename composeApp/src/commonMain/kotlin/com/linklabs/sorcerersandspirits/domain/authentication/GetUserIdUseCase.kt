package com.linklabs.sorcerersandspirits.domain.authentication

class GetUserIdUseCase(private val authenticationRepository: AuthenticationRepository) {
    operator fun invoke() = authenticationRepository.getUserId()
}
