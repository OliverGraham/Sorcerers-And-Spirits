package com.linklabs.sorcerersandspirits.domain.tavern.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository
import com.linklabs.sorcerersandspirits.domain.tavern.TavernData
import com.linklabs.sorcerersandspirits.domain.tavern.TavernRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetTavernDataFlowUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
    private val tavernRepository: TavernRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<TavernData?> {
        val userId = authenticationRepository.getUserId() ?: return flowOf(value = null)

        return profileRepository
            .getProfileFlow(userId = userId)
            .filterNotNull()
            .distinctUntilChanged(areEquivalent = ::isSameSession)
            .flatMapLatest { profile ->
                tavernRepository.getTavernTravelersFlow().distinctUntilChanged().map { travelers ->
                    TavernData(profile = profile, travelers = travelers)
                }
            }
    }

    private fun isSameSession(old: Profile, new: Profile): Boolean = old.userId == new.userId && old.displayName == new.displayName
}
