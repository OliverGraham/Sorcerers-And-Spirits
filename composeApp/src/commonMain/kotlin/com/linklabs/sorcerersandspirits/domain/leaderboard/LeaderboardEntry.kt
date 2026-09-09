package com.linklabs.sorcerersandspirits.domain.leaderboard

data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val rating: Long,
    val gauntletsWon: Long,
    val matchesWon: Long,
    val isCurrentUser: Boolean,
)
