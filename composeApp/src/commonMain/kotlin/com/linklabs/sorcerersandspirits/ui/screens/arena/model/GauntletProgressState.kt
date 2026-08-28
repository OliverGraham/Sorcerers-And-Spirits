package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.Gauntlet
import org.jetbrains.compose.resources.StringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.arena_finished_countdown_start_gauntlet
import sorcerersandspirits.composeapp.generated.resources.arena_finished_countdown_start_match
import sorcerersandspirits.composeapp.generated.resources.arena_finished_countdown_waiting_gauntlet
import sorcerersandspirits.composeapp.generated.resources.arena_finished_countdown_waiting_match
import sorcerersandspirits.composeapp.generated.resources.arena_finished_gauntlet_title
import sorcerersandspirits.composeapp.generated.resources.arena_finished_go_again
import sorcerersandspirits.composeapp.generated.resources.arena_finished_match_title
import sorcerersandspirits.composeapp.generated.resources.arena_finished_next_match

@Immutable
data class GauntletProgressState(
    val currentMatch: Int = 1,
    val matchesPerGauntlet: Int = Gauntlet.MATCHES_PER_GAUNTLET,
) {
    val isFinalMatch = currentMatch >= matchesPerGauntlet
    val stringResources = getGauntletProgressTextResources(
        isFinalMatch = isFinalMatch,
    )
}

@Immutable
data class GauntletProgressStringResources(
    val titleResource: StringResource,
    val continueActionResource: StringResource,
    val countdownWaitingPrefixResource: StringResource,
    val countdownStartPrefixResource: StringResource,
)

private fun getGauntletProgressTextResources(isFinalMatch: Boolean) = if (isFinalMatch) {
    GauntletProgressStringResources(
        titleResource = Res.string.arena_finished_gauntlet_title,
        continueActionResource = Res.string.arena_finished_go_again,
        countdownWaitingPrefixResource = Res.string.arena_finished_countdown_waiting_gauntlet,
        countdownStartPrefixResource = Res.string.arena_finished_countdown_start_gauntlet,
    )
} else {
    GauntletProgressStringResources(
        titleResource = Res.string.arena_finished_match_title,
        continueActionResource = Res.string.arena_finished_next_match,
        countdownWaitingPrefixResource = Res.string.arena_finished_countdown_waiting_match,
        countdownStartPrefixResource = Res.string.arena_finished_countdown_start_match,
    )
}
