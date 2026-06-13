package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus

data class CombatActionArguments(
    val arenaId: String,
    val playerMap: Map<String, ArenaPlayer.Selected>,
    val roundNumber: Int,
    val doublesBonus: DoublesBonus?,
)
