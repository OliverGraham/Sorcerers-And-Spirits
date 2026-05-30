package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer

data class CombatActionArguments(
    val arenaId: String,
    val userId: String,
    val attacker: ArenaPlayer.Selected,
    val defender: ArenaPlayer.Selected,
    val playerMap: Map<String, ArenaPlayer.Selected>,
    val roundNumber: Int,
    val originalTurnUserId: String,
)
