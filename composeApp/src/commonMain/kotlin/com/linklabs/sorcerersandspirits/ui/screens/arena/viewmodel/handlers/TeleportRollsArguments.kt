package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel.handlers

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction

data class TeleportRollsArguments(
    val mageUserId: String,
    val playerMap: Map<String, ArenaPlayer.Selected>,
    val roundNumber: Int,
    val nextCombatAction: NextCombatAction.AbilityRoll,
    val teleportOfNextCombatAction: AbilityAction.Passive.Teleport,
)
