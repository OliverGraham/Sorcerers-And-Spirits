package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.DiceRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.MatchParticipant
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

data class ActionOutcome(
    val messages: List<ArenaMessage>,
    val updatedPlayerMap: Map<UserId, ArenaPlayer.Selected>,
    val nextCombatAction: NextCombatAction,
    val currentTargetUserId: String?,
    val roundNumber: Int,
    val isNewRound: Boolean,
    val isMatchFinished: Boolean,
    val diceRoll: DiceRoll?,
    val participantMap: Map<UserId, MatchParticipant>,
)
