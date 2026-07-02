package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction.Passive.Teleport.TeleportDie
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class TeleportHandler(private val combatMutator: CombatMutator) {

    fun triggerTeleport() = combatMutator.triggerTeleport()

    fun handleTeleport(teleport: AbilityAction.Passive.Teleport) =
        combatMutator.handleTeleport(teleport = teleport)

    private fun CombatMutator.triggerTeleport() {
        addMessage(message = ArenaMessage.Teleport.Trigger)

        val purpose =
            AbilityAction.Passive.Teleport(
                attackerRoll = TeleportDie(userId = attackerUserId),
                defenderRoll = TeleportDie(userId = defenderUserId),
                attackerDoublesBonus = attackerDoublesBonus(),
            )

        setBothCombatantsRolling(teleport = purpose)
    }

    private fun CombatMutator.attackerDoublesBonus(): Int {
        val hasAttackerBonus = diceRoller.getDoublesBonus()?.userId == attackerUserId
        return if (hasAttackerBonus) 1 else 0
    }

    private fun CombatMutator.handleTeleport(teleport: AbilityAction.Passive.Teleport) {
        val updatedTeleport = rollActingUserDieOrNull(teleport = teleport) ?: return

        handleUpdatedTeleport(updatedTeleport = updatedTeleport)
    }

    private fun CombatMutator.handleUpdatedTeleport(
        updatedTeleport: AbilityAction.Passive.Teleport
    ) {
        val attackerRoll = updatedTeleport.attackerRoll.roll
        val defenderRoll = updatedTeleport.defenderRoll.roll

        if (attackerRoll != null && defenderRoll != null) {
            if (defenderRoll > attackerRoll) {
                handleTeleportSuccess()
            } else {
                handleTeleportFailure()
            }
        } else {
            setBothCombatantsRolling(teleport = updatedTeleport)
        }
    }

    /**
     * Both combatants hold the roll button at once and roll simultaneously; each dispatch rolls
     * only the acting player's own die (see [rollActingUserDieOrNull]), so neither side can roll
     * the other's. A computer combatant acts on its own thinking delay, exactly as a human would.
     */
    private fun CombatMutator.setBothCombatantsRolling(teleport: AbilityAction.Passive.Teleport) {
        setNextAbilityRoll(
            purpose = teleport,
            activeUserIds = setOf(attackerUserId, defenderUserId),
        )
    }

    private fun CombatMutator.rollActingUserDieOrNull(
        teleport: AbilityAction.Passive.Teleport
    ): AbilityAction.Passive.Teleport? {
        val isAttackerRolling = teleport.attackerRoll.userId == rollingUserId
        val rollingDie = if (isAttackerRolling) teleport.attackerRoll else teleport.defenderRoll

        if (rollingDie.userId != rollingUserId || rollingDie.roll != null) return null

        val bonus = if (isAttackerRolling) teleport.attackerDoublesBonus else 0
        val rolledDie = rollAndMessage(unrolledTeleportDie = rollingDie, bonus = bonus)
        return if (isAttackerRolling) {
            teleport.copy(attackerRoll = rolledDie)
        } else {
            teleport.copy(defenderRoll = rolledDie)
        }
    }

    private fun CombatMutator.rollAndMessage(
        unrolledTeleportDie: TeleportDie,
        bonus: Int,
    ): TeleportDie {
        val userId = unrolledTeleportDie.userId
        val result = diceRoller.roll1D6(userId = userId).die + bonus

        addMessage(message = ArenaMessage.Teleport.Roll(userId = userId, singleDie = result))

        return unrolledTeleportDie.copy(roll = result)
    }

    private fun CombatMutator.handleTeleportSuccess() {
        addMessage(message = ArenaMessage.Teleport.Result(didDefenderEscape = true))

        resolveTurnEnd()
    }

    private fun CombatMutator.handleTeleportFailure() {
        addMessage(message = ArenaMessage.Teleport.Result(didDefenderEscape = false))

        continueAttackWithHealthRoll()
    }
}
