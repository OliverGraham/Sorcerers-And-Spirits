package com.linklabs.sorcerersandspirits.domain.leaderboard.usecase

import com.linklabs.sorcerersandspirits.domain.authentication.AuthenticationRepository
import com.linklabs.sorcerersandspirits.domain.leaderboard.LeaderboardEntry
import com.linklabs.sorcerersandspirits.domain.profile.ProfileRepository

/**
 * Reads the top players — fetched ordered by Gauntlets won, the unit a stats write is attributed to
 * (SS-073) — and tags the signed-in player's own row so the screen can highlight it. The display
 * order/rank is decided by the UI (SS-132).
 */
class GetLeaderboardUseCase(
    private val profileRepository: ProfileRepository,
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke(): List<LeaderboardEntry> {
        val currentUserId = authenticationRepository.getUserId()

        return profileRepository.getTopProfiles(limit = LEADERBOARD_SIZE).map { profile ->
            LeaderboardEntry(
                displayName = profile.displayName,
                rating = profile.stats.rating,
                gauntletsWon = profile.stats.overall.gauntletsWon,
                matchesWon = profile.stats.overall.matchesWon,
                isCurrentUser = profile.userId == currentUserId,
            )
        }
    }

    private companion object {
        const val LEADERBOARD_SIZE = 50
    }
}
