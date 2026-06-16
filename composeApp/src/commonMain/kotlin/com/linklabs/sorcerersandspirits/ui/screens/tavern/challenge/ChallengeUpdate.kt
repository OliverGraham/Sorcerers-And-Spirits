package com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge

import com.linklabs.sorcerersandspirits.ui.screens.arena.ArenaScreenViewModelArguments
import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeState

internal sealed interface ChallengeUpdate {

    data class Ongoing(val challengeState: ChallengeState?) : ChallengeUpdate

    data class ReadyToStart(val arguments: ArenaScreenViewModelArguments) : ChallengeUpdate
}
