package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.arena.model.MatchParticipant

data class CombatActionArguments(
    val arenaId: String,
    val playerMap: Map<String, ArenaPlayer.Selected>,
    val participantMap: Map<String, MatchParticipant>,
    val roundNumber: Int,
    val doublesBonus: DoublesBonus?,
)
