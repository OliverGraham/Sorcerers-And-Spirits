package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class PersuadeHandler(private val combatMutator: CombatMutator) {

    fun triggerPersuade() = combatMutator.triggerPersuade()

    fun strumLute() = combatMutator.strumLute()

    fun charm(charm: AbilityAction.Passive.Persuade.Charm) = combatMutator.charm(charm = charm)

    private fun CombatMutator.triggerPersuade() {
        addMessage(message = ArenaMessage.Persuade.Trigger)

        setNextAbilityRoll(
            purpose = AbilityAction.Passive.Persuade.StrumLute,
            activeUserIds = setOf(element = defenderUserId),
        )
    }

    private fun CombatMutator.strumLute() {
        val result = diceRoller.roll1D6(userId = defenderUserId).total
        if (result < LULL) {
            handleStrumLuteFailure(persuadeDie = result)
        } else {
            handleStrumLuteSuccess(persuadeDie = result)
        }
    }

    private fun CombatMutator.handleStrumLuteFailure(persuadeDie: Int) {
        addMessage(
            message = ArenaMessage.Persuade.StrumLute(diceRoll = persuadeDie, isSuccessful = false)
        )

        continueAttackWithHealthRoll()
    }

    private fun CombatMutator.handleStrumLuteSuccess(persuadeDie: Int) {
        val isCharm = consciousPlayers.size >= 3 && persuadeDie == CHARM
        if (isCharm) {
            strumLuteToCharm(
                targetableUserIds =
                    consciousPlayers.map { it.userId }.toSet() - attackerUserId - defenderUserId
            )
        } else {
            strumLuteToLull(persuadeDie = persuadeDie)
        }
    }

    private fun CombatMutator.strumLuteToLull(persuadeDie: Int) {
        addMessage(
            message = ArenaMessage.Persuade.StrumLute(diceRoll = persuadeDie, isSuccessful = true)
        )
        resolveTurnEnd()
    }

    private fun CombatMutator.strumLuteToCharm(targetableUserIds: Set<String>) {
        addMessage(message = ArenaMessage.Persuade.CharmActivation)

        setNextAbilityRoll(
            purpose = AbilityAction.Passive.Persuade.Charm(targetableUserIds = targetableUserIds),
            activeUserIds = setOf(element = defenderUserId),
        )
    }

    private fun CombatMutator.charm(charm: AbilityAction.Passive.Persuade.Charm) {
        val selectedCharmTargetUserId = charm.selectedCharmTargetUserId ?: return

        addMessage(
            message =
                ArenaMessage.Persuade.Charm(
                    attackerUserId = attackerUserId,
                    defenderUserId = selectedCharmTargetUserId,
                )
        )

        setNextCombatAction(
            nextAction =
                NextCombatAction.NormalAttack(
                    roll = NormalAttackType.ArmorClass,
                    attackerUserId = attackerUserId,
                    defenderUserId = selectedCharmTargetUserId,
                    originalTurnUserId = originalTurnUserId,
                    activeUserIds = setOf(element = attackerUserId),
                )
        )
    }

    companion object {
        private const val LULL = 5
        private const val CHARM = 6
    }
}
