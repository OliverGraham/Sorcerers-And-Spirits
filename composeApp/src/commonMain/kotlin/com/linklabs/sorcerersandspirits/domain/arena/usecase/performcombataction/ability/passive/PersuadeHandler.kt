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
        addMessage(message = ArenaMessage.Persuade.Trigger(bardUserId = defenderUserId))

        setNextAbilityRoll(
            purpose = AbilityAction.Passive.Persuade.StrumLute,
            activeUserIds = setOf(element = defenderUserId),
        )
    }

    private fun CombatMutator.strumLute() {
        val result =
            diceRoller
                .roll1D6(userId = defenderUserId, dieColor = dieColorFor(userId = defenderUserId))
                .total
        if (result < abilityConfig.persuadeLullRoll) {
            handleStrumLuteFailure(persuadeDie = result)
        } else {
            handleStrumLuteSuccess(persuadeDie = result)
        }
    }

    private fun CombatMutator.handleStrumLuteFailure(persuadeDie: Int) {
        addMessage(
            message =
            ArenaMessage.Persuade.StrumLute(
                bardUserId = defenderUserId,
                diceRoll = persuadeDie,
                isSuccessful = false,
            ),
        )

        continueAttackWithHealthRoll()
    }

    private fun CombatMutator.handleStrumLuteSuccess(persuadeDie: Int) {
        val isCharm =
            consciousPlayers.size >= CHARM_MINIMUM_PLAYERS &&
                persuadeDie == abilityConfig.persuadeCharmRoll
        if (isCharm) {
            strumLuteToCharm(
                targetableUserIds =
                consciousPlayers.map { it.userId }.toSet() - attackerUserId - defenderUserId,
            )
        } else {
            strumLuteToLull(persuadeDie = persuadeDie)
        }
    }

    private fun CombatMutator.strumLuteToLull(persuadeDie: Int) {
        addMessage(
            message =
            ArenaMessage.Persuade.StrumLute(
                bardUserId = defenderUserId,
                diceRoll = persuadeDie,
                isSuccessful = true,
            ),
        )
        resolveTurnEnd()
    }

    private fun CombatMutator.strumLuteToCharm(targetableUserIds: Set<String>) {
        addMessage(
            message =
            ArenaMessage.Persuade.CharmActivation(
                bardUserId = defenderUserId,
                attackerUserId = attackerUserId,
                attackerClassName = attacker.className,
                attackerClassType = attacker.type,
            ),
        )

        val onlyLegalTargetUserId = targetableUserIds.singleOrNull()
        if (onlyLegalTargetUserId != null) {
            resolveCharm(charmTargetUserId = onlyLegalTargetUserId)
        } else {
            setNextAbilityRoll(
                purpose =
                AbilityAction.Passive.Persuade.Charm(targetableUserIds = targetableUserIds),
                activeUserIds = setOf(element = defenderUserId),
            )
        }
    }

    private fun CombatMutator.charm(charm: AbilityAction.Passive.Persuade.Charm) {
        val selectedCharmTargetUserId = charm.selectedCharmTargetUserId
        if (selectedCharmTargetUserId == null) {
            resolveTurnEnd()
            return
        }
        resolveCharm(charmTargetUserId = selectedCharmTargetUserId)
    }

    private fun CombatMutator.resolveCharm(charmTargetUserId: String) {
        val charmTarget = consciousPlayers.firstOrNull { it.userId == charmTargetUserId }
        if (charmTarget == null) {
            resolveTurnEnd()
            return
        }
        val charmTargetClass = charmTarget.characterClass

        redirectCurrentTarget()

        addMessage(
            message =
            ArenaMessage.Persuade.Charm(
                bardUserId = defenderUserId,
                attackerUserId = attackerUserId,
                attackerClassName = attacker.className,
                attackerClassType = attacker.type,
                defenderUserId = charmTargetUserId,
                defenderClassName = charmTargetClass.className,
                defenderClassType = charmTargetClass.type,
            ),
        )

        setNextCombatAction(
            nextAction =
            NextCombatAction.NormalAttack(
                roll = NormalAttackType.ArmorClass,
                attackTurn = currentAttackTurn(),
                defenderUserId = charmTargetUserId,
                activeUserIds = setOf(element = attackerUserId),
            ),
        )
    }

    companion object {
        private const val CHARM_MINIMUM_PLAYERS = 3
    }
}
