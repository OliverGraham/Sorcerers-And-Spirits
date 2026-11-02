package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.computeropponent.ComputerOpponentHandler
import com.linklabs.sorcerersandspirits.domain.arena.computeropponent.ComputerOpponentIdentity
import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

class StartComputerOpponentMatchUseCase(
    private val authenticationRepository: AuthenticationRepository,
    private val profileRepository: ProfileRepository,
    private val computerOpponentHandler: ComputerOpponentHandler,
    private val computerOpponentIdentity: ComputerOpponentIdentity,
) {
    suspend operator fun invoke(): ComputerOpponentMatch? {
        val userId = authenticationRepository.getUserId() ?: return null
        val profile = profileRepository.getProfile(userId = userId) ?: return null

        computerOpponentHandler.start(userDisplayName = profile.displayName, userId = userId)

        return ComputerOpponentMatch(
            arenaId = "computer-opponent-match-$userId",
            computerOpponentUserId = computerOpponentIdentity.userId,
            expectedNumberOfPlayers = COMPUTER_OPPONENT_PLAYER_COUNT,
        )
    }
}

data class ComputerOpponentMatch(
    val arenaId: String,
    val computerOpponentUserId: String,
    val expectedNumberOfPlayers: Int,
)

private const val COMPUTER_OPPONENT_PLAYER_COUNT = 2
