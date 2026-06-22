package com.linklabs.sorcerersandspirits.ui.screens.leaderboard

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.leaderboard.LeaderboardEntry

@Immutable data class LeaderboardState(val entries: List<LeaderboardRowState>)

@Immutable
data class LeaderboardRowState(
    val rank: Int,
    val displayName: String,
    val rating: Long,
    val gauntletsWon: Long,
    val matchesWon: Long,
    val isCurrentUser: Boolean,
)

internal fun LeaderboardEntry.toRowState() =
    LeaderboardRowState(
        rank = rank,
        displayName = displayName,
        rating = rating,
        gauntletsWon = gauntletsWon,
        matchesWon = matchesWon,
        isCurrentUser = isCurrentUser,
    )
