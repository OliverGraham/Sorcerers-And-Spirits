package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.mirrorimage

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

// TODO: Fix everything in this file
internal class MirrorImageRerollOutcome(
    private val combatMutator: CombatMutator,
    private val firstDie: Int,
    private val secondDie: Int,
    private val rerollFirstDie: Boolean,
    private val triggeredByArmorRoll: Boolean,
) {
    operator fun invoke(): CombatActionUpdates {
        val roll = roll1D6() - 1
        val newFirstDie = if (rerollFirstDie) roll else firstDie
        val newSecondDie = if (!rerollFirstDie) roll else secondDie

        combatMutator.addMessage(
            message =
                OutgoingArenaMessage.MirrorImage.Reroll(
                    firstDie = newFirstDie,
                    secondDie = newSecondDie,
                )
        )

        combatMutator.markMirrorImageAsUsed()

        val diceTotal = newFirstDie + newSecondDie

        return if (triggeredByArmorRoll) {
            resolveArmorRoll(diceTotal, newFirstDie, newSecondDie)
        } else {
            resolveHealthRoll(diceTotal, newFirstDie, newSecondDie)
        }
    }

    private fun resolveArmorRoll(
        diceTotal: Int,
        firstDie: Int,
        secondDie: Int,
    ): CombatActionUpdates {
        val defenderAc = combatMutator.getDefenderAc()
        return if (diceTotal >= defenderAc) {
            combatMutator.addNormalRollMessage(
                firstDie,
                secondDie,
                NormalRollMessageType.ArmorClassHit,
            )
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutator.attackerUserId,
                nextStep = NextCombatAction.NormalAttackRoll.Health,
            )
        } else {
            combatMutator.addNormalRollMessage(
                firstDie,
                secondDie,
                NormalRollMessageType.ArmorClassMiss,
            )
            combatMutator.resolveTurnEnd()
        }
    }

    private fun resolveHealthRoll(
        diceTotal: Int,
        firstDie: Int,
        secondDie: Int,
    ): CombatActionUpdates {
        val defenderHp = combatMutator.getDefenderHp()
        return if (diceTotal >= defenderHp) {
            combatMutator.addNormalRollMessage(firstDie, secondDie, NormalRollMessageType.Knockout)
            combatMutator.updateDefenderClass { it.copy(hitPoints = 0) }
            combatMutator.resolveTurnEnd()
        } else {
            combatMutator.addNormalRollMessage(firstDie, secondDie, NormalRollMessageType.Damage)
            combatMutator.updateDefenderClass { it.copy(hitPoints = it.hitPoints - diceTotal) }
            combatMutator.resolveTurnEnd()
        }
    }

    private fun CombatMutator.markMirrorImageAsUsed() {
        updateDefenderClass { playerClass ->
            val mirrorImage = playerClass.defensiveAbility as DefensiveAbility.MirrorImage
            playerClass.copy(defensiveAbility = mirrorImage.copy(usedThisAttack = true))
        }
    }

    private fun CombatMutator.getDefenderAc(): Int =
        updatedPlayerMap.getValue(defenderUserId).characterClass.armorClass

    private fun CombatMutator.getDefenderHp(): Int =
        updatedPlayerMap.getValue(defenderUserId).characterClass.hitPoints

    private fun CombatMutator.addNormalRollMessage(
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
