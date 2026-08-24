package com.linklabs.sorcerersandspirits.data.arena.utilities.applycombatresult

import com.linklabs.sorcerersandspirits.data.arena.model.AbilityActionEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.NextCombatActionEntity
import com.linklabs.sorcerersandspirits.data.arena.model.toEntity
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction

/** Add incoming teleport roll into existing entry in database, if applicable. */
internal fun ArenaMatchEntity.Combat.getUpdatedTeleportCombatActionOrNull(
    nextCombatAction: NextCombatAction
): NextCombatActionEntity? {
    val currentTeleportEntity = getCurrentTeleportOrNull() ?: return null
    val nextActionAndTeleport = nextCombatAction.getNextActionAndTeleportOrNull() ?: return null

    val nextTeleport = nextActionAndTeleport.nextCombatActionsTeleport
    val isOnlyOneTeleportIn =
        nextTeleport.isOnlyOneTeleportRollIn(currentTeleportEntity = currentTeleportEntity)
    if (!isOnlyOneTeleportIn) return null

    val mergedTeleport =
        nextTeleport.mergeWithCurrent(currentTeleportEntity = currentTeleportEntity)

    return nextActionAndTeleport.nextCombatAction.copy(purpose = mergedTeleport).toEntity()
}

private fun NextCombatAction.getNextActionAndTeleportOrNull(): AbilityRollAndTeleport? {
    if (this !is NextCombatAction.AbilityRoll) return null

    val teleport = purpose
    if (teleport !is AbilityAction.Passive.Teleport) return null

    return AbilityRollAndTeleport(nextCombatAction = this, nextCombatActionsTeleport = teleport)
}

private fun AbilityAction.Passive.Teleport.isOnlyOneTeleportRollIn(
    currentTeleportEntity: AbilityActionEntity.PassiveEntity.TeleportEntity
): Boolean {
    val isAttackerRollMissing = attackerRoll.roll == null
    val isDefenderRollMissing = defenderRoll.roll == null

    return when {
        isAttackerRollMissing && isDefenderRollMissing -> false
        !isAttackerRollMissing && !isDefenderRollMissing -> false
        isAttackerRollMissing -> currentTeleportEntity.attackerRoll.roll != null
        else -> currentTeleportEntity.defenderRoll.roll != null
    }
}

private fun ArenaMatchEntity.Combat.getCurrentTeleportOrNull():
    AbilityActionEntity.PassiveEntity.TeleportEntity? {
    if (nextCombatActionEntity !is NextCombatActionEntity.AbilityRollEntity) return null

    return nextCombatActionEntity.purpose as? AbilityActionEntity.PassiveEntity.TeleportEntity
}

private fun AbilityAction.Passive.Teleport.mergeWithCurrent(
    currentTeleportEntity: AbilityActionEntity.PassiveEntity.TeleportEntity
) =
    copy(
        attackerRoll =
            attackerRoll.copy(roll = attackerRoll.roll ?: currentTeleportEntity.attackerRoll.roll),
        defenderRoll =
            defenderRoll.copy(roll = defenderRoll.roll ?: currentTeleportEntity.defenderRoll.roll),
    )

private data class AbilityRollAndTeleport(
    val nextCombatAction: NextCombatAction.AbilityRoll,
    val nextCombatActionsTeleport: AbilityAction.Passive.Teleport,
)
