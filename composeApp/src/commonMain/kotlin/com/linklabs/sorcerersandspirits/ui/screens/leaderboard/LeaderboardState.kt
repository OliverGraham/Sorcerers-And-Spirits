package com.linklabs.sorcerersandspirits.ui.screens.leaderboard

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.leaderboard.LeaderboardEntry

@Immutable
data class LeaderboardState(val entries: List<LeaderboardRowState>, val sort: LeaderboardSort)

@Immutable
data class LeaderboardRowState(
    val rank: Int,
    val displayName: String,
    val rating: Long,
    val gauntletsWon: Long,
    val matchesWon: Long,
    val isCurrentUser: Boolean,
)

/** The field the player has chosen to order the (already-fetched) leaderboard rows by. */
enum class LeaderboardSort {
    Rating,
    GauntletsWon,
    MatchesWon,
}

/**
 * Orders the fetched entries by [sort] (descending) and assigns the displayed 1-based rank from
 * that order. Ties keep the fetched order (top players by Gauntlets won) since the sort is stable.
 */
internal fun List<LeaderboardEntry>.toRowStates(sort: LeaderboardSort): List<LeaderboardRowState> =
    sortedByDescending { entry -> entry.sortValue(sort = sort) }
        .mapIndexed { index, entry -> entry.toRowState(rank = index + 1) }

private fun LeaderboardEntry.sortValue(sort: LeaderboardSort): Long =
    when (sort) {
        LeaderboardSort.Rating -> rating
        LeaderboardSort.GauntletsWon -> gauntletsWon
        LeaderboardSort.MatchesWon -> matchesWon
    }

private fun LeaderboardEntry.toRowState(rank: Int) =
    LeaderboardRowState(
        rank = rank,
        displayName = displayName,
        rating = rating,
        gauntletsWon = gauntletsWon,
        matchesWon = matchesWon,
        isCurrentUser = isCurrentUser,
    )
