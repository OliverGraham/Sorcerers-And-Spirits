package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction.Passive.Teleport.TeleportDie
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DiePosition

internal class TeleportHandler(private val combatMutator: CombatMutator) {

    fun triggerTeleport() = combatMutator.triggerTeleport()

    fun handleTeleport(teleport: AbilityAction.Passive.Teleport) = combatMutator.handleTeleport(teleport = teleport)

    private fun CombatMutator.triggerTeleport() {
        addMessage(message = ArenaMessage.Teleport.Trigger(mageUserId = defenderUserId))

        val purpose =
            AbilityAction.Passive.Teleport(
                attackerRoll =
                TeleportDie(userId = attackerUserId, diePosition = DiePosition.First),
                defenderRoll =
                TeleportDie(userId = defenderUserId, diePosition = DiePosition.Second),
                attackerDoublesBonus = attackerDoublesBonus(),
            )

        setBothCombatantsRolling(teleport = purpose)
    }

    private fun CombatMutator.attackerDoublesBonus(): Int {
        val hasAttackerBonus = diceRoller.getDoublesBonus()?.userId == attackerUserId
        return if (hasAttackerBonus) 1 else 0
    }

    private fun CombatMutator.handleTeleport(teleport: AbilityAction.Passive.Teleport) {
        val areBothRollsIn =
            teleport.attackerRoll.roll != null && teleport.defenderRoll.roll != null
        if (areBothRollsIn) {
            handleUpdatedTeleport(updatedTeleport = teleport)
            return
        }

        val updatedTeleport = rollActingUserDieOrNull(teleport = teleport)
        if (updatedTeleport == null) {
            setBothCombatantsRolling(teleport = teleport)
            return
        }

        handleUpdatedTeleport(updatedTeleport = updatedTeleport)
    }

    private fun CombatMutator.handleUpdatedTeleport(
        updatedTeleport: AbilityAction.Passive.Teleport,
    ) {
        val attackerRoll = updatedTeleport.attackerRoll.roll
        val defenderRoll = updatedTeleport.defenderRoll.roll
        if (attackerRoll != null && defenderRoll != null) {
            handleTeleportOutcome(
                updatedTeleport = updatedTeleport,
                attackerRoll = attackerRoll,
                defenderRoll = defenderRoll,
            )
        } else {
            setBothCombatantsRolling(teleport = updatedTeleport)
        }
    }

    private fun CombatMutator.handleTeleportOutcome(
        updatedTeleport: AbilityAction.Passive.Teleport,
        attackerRoll: Int,
        defenderRoll: Int,
    ) {
        val mageUserId = updatedTeleport.defenderRoll.userId

        when {
            defenderRoll > attackerRoll -> handleTeleportSuccess(mageUserId = mageUserId)
            defenderRoll < attackerRoll -> handleTeleportFailure(mageUserId = mageUserId)
            else -> rerollTeleport(teleport = updatedTeleport)
        }
    }

    private fun CombatMutator.rerollTeleport(teleport: AbilityAction.Passive.Teleport) {
        addMessage(message = ArenaMessage.Teleport.Tie(mageUserId = teleport.defenderRoll.userId))

        val resetTeleport =
            teleport.copy(
                attackerRoll = teleport.attackerRoll.copy(roll = null),
                defenderRoll = teleport.defenderRoll.copy(roll = null),
                attackerDoublesBonus = 0,
            )

        setBothCombatantsRolling(teleport = resetTeleport)
    }

    private fun CombatMutator.setBothCombatantsRolling(teleport: AbilityAction.Passive.Teleport) {
        setNextAbilityRoll(
            purpose = teleport,
            activeUserIds = setOf(attackerUserId, defenderUserId),
        )
    }

    private fun CombatMutator.rollActingUserDieOrNull(
        teleport: AbilityAction.Passive.Teleport,
    ): AbilityAction.Passive.Teleport? {
        val isAttackerRolling = teleport.attackerRoll.userId == rollingUserId
        val rollingDie = if (isAttackerRolling) teleport.attackerRoll else teleport.defenderRoll

        if (rollingDie.userId != rollingUserId || rollingDie.roll != null) return null

        val bonus = if (isAttackerRolling) teleport.attackerDoublesBonus else 0
        val rolledDie =
            rollAndMessage(
                unrolledTeleportDie = rollingDie,
                bonus = bonus,
                mageUserId = teleport.defenderRoll.userId,
                className = if (isAttackerRolling) attacker.className else defender.className,
                classType = if (isAttackerRolling) attacker.type else defender.type,
            )
        return if (isAttackerRolling) {
            teleport.copy(attackerRoll = rolledDie)
        } else {
            teleport.copy(defenderRoll = rolledDie)
        }
    }

    private fun CombatMutator.rollAndMessage(
        unrolledTeleportDie: TeleportDie,
        bonus: Int,
        mageUserId: String,
        className: String,
        classType: CharacterClassType,
    ): TeleportDie {
        val userId = unrolledTeleportDie.userId
        val result =
            diceRoller
                .roll1D6(
                    userId = userId,
                    diePosition = unrolledTeleportDie.diePosition,
                    disablesSibling = false,
                    dieColor = dieColorFor(userId = userId),
                )
                .die + bonus

        addMessage(
            message =
            ArenaMessage.Teleport.Roll(
                userId = userId,
                singleDie = result,
                mageUserId = mageUserId,
                className = className,
                classType = classType,
            ),
        )

        return unrolledTeleportDie.copy(roll = result)
    }

    private fun CombatMutator.handleTeleportSuccess(mageUserId: String) {
        addMessage(
            message =
            ArenaMessage.Teleport.Result(didDefenderEscape = true, mageUserId = mageUserId),
        )

        resolveTurnEnd()
    }

    private fun CombatMutator.handleTeleportFailure(mageUserId: String) {
        addMessage(
            message =
            ArenaMessage.Teleport.Result(didDefenderEscape = false, mageUserId = mageUserId),
        )

        continueAttackWithHealthRoll()
    }
}
