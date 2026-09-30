package com.linklabs.sorcerersandspirits.domain.profile.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.PendingStatsStore
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

class DeleteProfileUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
    private val pendingStatsStore: PendingStatsStore,
) {
    suspend operator fun invoke() {
        val userId = authenticationRepository.getUserId() ?: return

        profileRepository.deleteProfile(userId = userId)
        pendingStatsStore.clear()
        authenticationRepository.deleteAccount()
    }
}
