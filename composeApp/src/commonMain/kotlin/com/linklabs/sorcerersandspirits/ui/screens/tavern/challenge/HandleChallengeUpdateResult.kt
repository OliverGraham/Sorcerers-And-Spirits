package com.linklabs.sorcerersandspirits.ui.screens.tavern.challenge

import com.linklabs.sorcerersandspirits.ui.screens.tavern.ChallengeState
import com.linklabs.sorcerersandspirits.ui.screens.tavern.TavernEvent

internal data class HandleChallengeUpdateResult(
    val challengeState: ChallengeState,
    val tavernEvent: TavernEvent?,
)
