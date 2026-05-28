package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.mirrorimage

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

// TODO: Huh? - this is so wrong
internal class MirrorImagePassOutcome(
    private val combatMutator: CombatMutator,
    private val firstDie: Int,
    private val secondDie: Int,
    private val triggeredByArmorRoll: Boolean,
) {
    operator fun invoke(): CombatActionUpdates {
        combatMutator.addMessage(message = OutgoingArenaMessage.MirrorImage.Pass)

        combatMutator.markMirrorImageAsUsed()

        return if (triggeredByArmorRoll) {
            // Hit stands, move to damage roll
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutator.attackerUserId,
                nextStep = NextCombatAction.NormalAttackRoll.Health,
            )
        } else {
            // Damage roll stands
            val diceTotal = firstDie + secondDie
            val defenderHp = combatMutator.getDefenderHp()

            if (diceTotal >= defenderHp) {
                combatMutator.addNormalRollMessage(
                    attackerUserId = combatMutator.attackerUserId,
                    defenderUserId = combatMutator.defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = NormalRollMessageType.Knockout,
                )
                combatMutator.updateDefenderClass { it.copy(hitPoints = 0) }
                combatMutator.resolveTurnEnd()
            } else {
                combatMutator.addNormalRollMessage(
                    attackerUserId = combatMutator.attackerUserId,
                    defenderUserId = combatMutator.defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = NormalRollMessageType.Damage,
                )
                combatMutator.updateDefenderClass { it.copy(hitPoints = it.hitPoints - diceTotal) }
                combatMutator.resolveTurnEnd()
            }
        }
    }

    private fun CombatMutator.markMirrorImageAsUsed() {
        updateDefenderClass { playerClass ->
            val mirrorImage = playerClass.defensiveAbility as DefensiveAbility.MirrorImage
            playerClass.copy(defensiveAbility = mirrorImage.copy(usedThisAttack = true))
        }
    }

    private fun CombatMutator.getDefenderHp(): Int =
        updatedPlayerMap.getValue(defenderUserId).characterClass.hitPoints

    private fun CombatMutator.addNormalRollMessage(
        attackerUserId: String,
        defenderUserId: String,
        firstDie: Int,
        secondDie: Int,
        type: NormalRollMessageType,
    ) {
        addMessage(
            message =
                OutgoingArenaMessage.NormalRoll(
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = type,
                )
        )
    }
}
