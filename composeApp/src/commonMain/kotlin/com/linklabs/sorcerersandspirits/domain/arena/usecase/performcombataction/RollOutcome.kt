package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

data class RollOutcome(
    val messages: List<OutgoingArenaMessage>,
    val updatedPlayerMap: Map<UserId, ArenaPlayer.Selected>,
    val nextActivePlayerUserId: String,
    val nextStep: NextCombatAction,
)
