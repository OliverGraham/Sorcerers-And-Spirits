package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.NormalAttackRollHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6

internal class PersuadeHandler(private val combatMutator: CombatMutator) {

    fun triggerPersuade(attackerRollTotal: Int) =
        combatMutator.triggerPersuade(attackerRollTotal = attackerRollTotal)

    fun strumLute() = combatMutator.strumLute()

    fun charm(charm: AbilityRollPurpose.Passive.Persuade.Charm) = combatMutator.charm(charm = charm)

    private fun CombatMutator.triggerPersuade(attackerRollTotal: Int) {
        addMessage(message = OutgoingArenaMessage.Persuade.Trigger)

        val purpose =
            AbilityRollPurpose.Passive.Persuade.StrumLute(
                attackerRollTotal = attackerRollTotal,
                bardUserId = defenderUserId,
            )
        setNextCombatAction(nextAction = NextCombatAction.AbilityRoll(purpose = purpose))
    }

    private fun CombatMutator.strumLute() {
        val persuadeDie = roll1D6()
        if (persuadeDie < LULL) {
            handleStrumLuteFailure(persuadeDie = persuadeDie)
        } else {
            handleStrumLuteSuccess(persuadeDie = persuadeDie)
        }
    }

    private fun CombatMutator.handleStrumLuteFailure(persuadeDie: Int) {
        addMessage(
            message =
                OutgoingArenaMessage.Persuade.StrumLute(
                    diceRoll = persuadeDie,
                    isSuccessful = false,
                )
        )

        val healthAttackRoll =
            NormalAttackRoll.Health(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
            )

        setNextCombatAction(nextAction = NextCombatAction.NormalAttack(roll = healthAttackRoll))
    }

    private fun CombatMutator.handleStrumLuteSuccess(persuadeDie: Int) {
        val isCharm = consciousPlayers.size >= 2 && persuadeDie == CHARM
        if (isCharm) {
            strumLuteToCharm(
                charmTargetUserIds =
                    consciousPlayers.map { it.userId }.toSet() - attackerUserId - defenderUserId
            )
        } else {
            strumLuteToLull(persuadeDie = persuadeDie)
        }
    }

    private fun CombatMutator.strumLuteToLull(persuadeDie: Int) {
        // TODO Persuade: "Bard rolled a 5 and stopped $Attacker's attack!"
        addMessage(
            message =
                OutgoingArenaMessage.Persuade.StrumLute(diceRoll = persuadeDie, isSuccessful = true)
        )
        resolveTurnEnd()
    }

    private fun CombatMutator.strumLuteToCharm(charmTargetUserIds: Set<String>) {
        // TODO Persuade: "Bard rolled a 6, stopped $Attacker's attack and now forcing $Attacker to
        //  fight someone else!"
        addMessage(message = OutgoingArenaMessage.Persuade.CharmActivation)

        val purpose =
            AbilityRollPurpose.Passive.Persuade.Charm(
                attackerUserId = attackerUserId,
                bardUserId = defenderUserId,
                charmTargetUserIds = charmTargetUserIds,
            )

        setNextCombatAction(nextAction = NextCombatAction.AbilityRoll(purpose = purpose))
    }

    private fun CombatMutator.charm(charm: AbilityRollPurpose.Passive.Persuade.Charm) {
        val selectedCharmTargetUserId = charm.selectedCharmTargetUserId ?: return

        // TODO Persuade: "Under Bard's control, $Attacker attacks $Defender!"
        addMessage(
            message =
                OutgoingArenaMessage.Persuade.Charm(
                    attackerUserId = attackerUserId,
                    defenderUserId = selectedCharmTargetUserId,
                )
        )

        val armorClassAttackRoll =
            NormalAttackRoll.ArmorClass(
                attackerUserId = attackerUserId,
                defenderUserId = selectedCharmTargetUserId,
            )
        NormalAttackRollHandler.handleNormalRoll(
            attacker = attacker,
            defender = defender,
            currentAction = CurrentCombatAction.NormalAttack(roll = armorClassAttackRoll),
            combatMutator = combatMutator,
        )
    }

    companion object {
        private const val LULL = 5
        private const val CHARM = 6
    }
}
