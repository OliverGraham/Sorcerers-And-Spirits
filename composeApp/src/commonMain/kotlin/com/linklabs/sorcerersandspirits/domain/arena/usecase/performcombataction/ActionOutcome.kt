package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.DoublesBonus
import com.linklabs.sorcerersandspirits.domain.arena.model.MyDiceRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

data class ActionOutcome(
    val messages: List<ArenaMessage>,
    val updatedPlayerMap: Map<UserId, ArenaPlayer.Selected>,
    val nextCombatAction: NextCombatAction,
    val roundNumber: Int,
    val isNewRound: Boolean,
    val doublesBonus: DoublesBonus?,
    val isMirrorImagePenaltyActive: Boolean,
    val isMatchFinished: Boolean,
    val myDiceRoll: MyDiceRoll?,
)
