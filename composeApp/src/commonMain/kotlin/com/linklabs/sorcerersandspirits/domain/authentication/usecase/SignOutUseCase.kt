package com.linklabs.sorcerersandspirits.domain.authentication.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository

class SignOutUseCase(private val authenticationRepository: AuthenticationRepository) {
    suspend operator fun invoke() = authenticationRepository.signOut()
}
