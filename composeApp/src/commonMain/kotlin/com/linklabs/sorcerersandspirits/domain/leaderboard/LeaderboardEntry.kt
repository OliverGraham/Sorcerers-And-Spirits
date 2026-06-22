package com.linklabs.sorcerersandspirits.domain.leaderboard

/**
 * A single leaderboard player. Rows are ranked at display time (the UI can re-sort client-side, see
 * SS-132), so no rank is carried here. [isCurrentUser] marks the signed-in player's own row so the
 * screen can highlight it.
 */
data class LeaderboardEntry(
    val displayName: String,
    val rating: Long,
    val gauntletsWon: Long,
    val matchesWon: Long,
    val isCurrentUser: Boolean,
)
