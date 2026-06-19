package com.linklabs.sorcerersandspirits.domain.leaderboard

/**
 * A single ranked row of the leaderboard. [rank] is 1-based and assigned by ordering,
 * [isCurrentUser] marks the signed-in player's own row so the screen can highlight it.
 */
data class LeaderboardEntry(
    val rank: Int,
    val displayName: String,
    val gauntletsWon: Long,
    val matchesWon: Long,
    val isCurrentUser: Boolean,
)
