package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.MirrorImageStatus
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

class MirrorImageRerollOutcome(
    private val combatMutations: CombatMutations,
    private val firstDie: Int,
    private val secondDie: Int,
    private val rerollFirstDie: Boolean,
    private val triggeredByArmorRoll: Boolean,
) {
    operator fun invoke(): CombatActionUpdates {
        val roll = roll1D6() - 1
        val newFirstDie = if (rerollFirstDie) roll else firstDie
        val newSecondDie = if (!rerollFirstDie) roll else secondDie

        combatMutations.addMessage(
            message =
                OutgoingMessage.MirrorImage(
                    illusionistUserId = combatMutations.defenderUserId,
                    status = MirrorImageStatus.Rerolled,
                )
        )

        combatMutations.markMirrorImageAsUsed()

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
        val defenderAc = combatMutations.getDefenderAc()
        return if (diceTotal >= defenderAc) {
            combatMutations.addNormalRollMessage(
                firstDie,
                secondDie,
                NormalRollMessageType.ArmorClassHit,
            )
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutations.attackerUserId,
                nextStep = NextCombatAction.NormalAttackRoll.HealthRoll,
            )
        } else {
            combatMutations.addNormalRollMessage(
                firstDie,
                secondDie,
                NormalRollMessageType.ArmorClassMiss,
            )
            combatMutations.resolveTurnEnd()
        }
    }

    private fun resolveHealthRoll(
        diceTotal: Int,
        firstDie: Int,
        secondDie: Int,
    ): CombatActionUpdates {
        val defenderHp = combatMutations.getDefenderHp()
        return if (diceTotal >= defenderHp) {
            combatMutations.addNormalRollMessage(
                firstDie,
                secondDie,
                NormalRollMessageType.Knockout,
            )
            combatMutations.updateDefenderClass { it.copy(hitPoints = 0) }
            combatMutations.resolveTurnEnd()
        } else {
            combatMutations.addNormalRollMessage(firstDie, secondDie, NormalRollMessageType.Damage)
            combatMutations.updateDefenderClass { it.copy(hitPoints = it.hitPoints - diceTotal) }
            combatMutations.resolveTurnEnd()
        }
    }

    private fun CombatMutations.markMirrorImageAsUsed() {
        updateDefenderClass { playerClass ->
            val mirrorImage = playerClass.defensiveAbility as DefensiveAbility.MirrorImage
            playerClass.copy(defensiveAbility = mirrorImage.copy(usedThisAttack = true))
        }
    }

    private fun CombatMutations.getDefenderAc(): Int =
        updatedPlayerMap.getValue(defenderUserId).characterClass.armorClass

    private fun CombatMutations.getDefenderHp(): Int =
        updatedPlayerMap.getValue(defenderUserId).characterClass.hitPoints

    private fun CombatMutations.addNormalRollMessage(
        firstDie: Int,
        secondDie: Int,
        type: NormalRollMessageType,
    ) {
        addMessage(
            message =
                OutgoingMessage.NormalRoll(
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = type,
                )
        )
    }
}
