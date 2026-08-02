package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

// TODO: Delete this - use ArenaMessageState
@Immutable
data class CombatSeating(
    val topSeatUserId: String?,
    val bottomSeatUserId: String?,
    val headIconUserIds: List<String>,
)

// TODO: This "seating" terminology is not good. I just don't like it
internal fun ArenaState.Combat.seatingFor(
    targetUserId: String,
    isMatchEnding: Boolean,
    collapsingTargetUserId: String?,
): CombatSeating {
    val actorUserId = actorUserId()
    val forcedSecondSeatUserId =
        (collapsingTargetUserId ?: holyShieldDefenderUserId())?.takeIf {
            it in playerMap.keys && it != actorUserId
        }
    val secondSeatUserId =
        forcedSecondSeatUserId
            ?: seatedTargetUserId(targetUserId = targetUserId, isMatchEnding = isMatchEnding)
    val seatedUserIds = setOfNotNull(actorUserId, secondSeatUserId)
    val (topSeatUserId, bottomSeatUserId) = assignSeats(seatedUserIds = seatedUserIds)

    return CombatSeating(
        topSeatUserId = topSeatUserId,
        bottomSeatUserId = bottomSeatUserId,
        headIconUserIds = playerMap.keys.filter { it !in seatedUserIds },
    )
}

private fun ArenaState.Combat.holyShieldDefenderUserId(): String? {
    val abilityRoll = combatActionState as? CombatActionState.AbilityRoll ?: return null
    val isHolyShieldRoll = abilityRoll.action is AbilityActionState.Passive.HolyShield
    return if (isHolyShieldRoll) abilityRoll.defenderUserId else null
}

internal fun ArenaState.Combat.seatedPlayer(seatUserId: String?): PlayerState.Selected? {
    if (seatUserId == null) return null
    return playerMap[seatUserId]
}

private fun ArenaState.Combat.seatedTargetUserId(
    targetUserId: String,
    isMatchEnding: Boolean,
): String? {
    val target = playerMap[targetUserId] ?: return null
    val isKnockedOutMidMatch = !target.characterClass.isConscious && !isMatchEnding
    if (isKnockedOutMidMatch) return null
    return if (target.userId == actorUserId()) null else target.userId
}

private fun ArenaState.Combat.actorUserId(): String =
    when (val action = combatActionState) {
        is CombatActionState.Idle -> action.activeUserIds.first()
        is CombatActionState.NormalAttack -> action.attackTurn.attackerUserId
        is CombatActionState.AbilityRoll -> action.attackTurn.attackerUserId
    }

private fun ArenaState.Combat.assignSeats(seatedUserIds: Set<String>): Pair<String?, String?> {
    if (userPlayerId in seatedUserIds) {
        val seatedOpponentUserId = seatedUserIds.singleOrNull { it != userPlayerId }
        return seatedOpponentUserId to userPlayerId
    }

    val orderedUserIds = seatedUserIds.sorted()
    return orderedUserIds.firstOrNull() to orderedUserIds.getOrNull(index = 1)
}
