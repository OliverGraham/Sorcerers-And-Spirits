package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.utilities.UserId
import kotlin.collections.set

data class CombatMutations(
    val updatedPlayerMap: Map<UserId, ArenaPlayer.Selected>,
    val messages: List<OutgoingMessage>,
)
