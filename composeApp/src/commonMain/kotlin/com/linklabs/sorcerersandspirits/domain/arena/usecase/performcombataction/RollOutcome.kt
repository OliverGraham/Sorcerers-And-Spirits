package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

data class RollOutcome(
    val messages: List<OutgoingMessage>,
    val updatedPlayerMap: Map<UserId, ArenaPlayer.Selected>,
    val nextActivePlayerUserId: String,
    val nextStep: NextCombatAction,
)
