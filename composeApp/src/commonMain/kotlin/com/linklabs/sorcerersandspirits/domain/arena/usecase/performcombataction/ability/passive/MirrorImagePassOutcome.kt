package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.MirrorImageStatus
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatMutations
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

// TODO: Huh?
class MirrorImagePassOutcome(
    private val combatMutations: CombatMutations,
    private val firstDie: Int,
    private val secondDie: Int,
    private val triggeredByArmorRoll: Boolean,
) {
    operator fun invoke(): CombatActionUpdates {
        combatMutations.addMessage(
            message =
                OutgoingMessage.MirrorImage(
                    illusionistUserId = combatMutations.defenderUserId,
                    status = MirrorImageStatus.Passed,
                )
        )

        combatMutations.markMirrorImageAsUsed()

        return if (triggeredByArmorRoll) {
            // Hit stands, move to damage roll
            CombatActionUpdates(
                nextActivePlayerUserId = combatMutations.attackerUserId,
                nextStep = NextCombatAction.NormalAttackRoll.HealthRoll,
            )
        } else {
            // Damage roll stands
            val diceTotal = firstDie + secondDie
            val defenderHp = combatMutations.getDefenderHp()

            if (diceTotal >= defenderHp) {
                combatMutations.addNormalRollMessage(
                    attackerUserId = combatMutations.attackerUserId,
                    defenderUserId = combatMutations.defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = NormalRollMessageType.Knockout,
                )
                combatMutations.updateDefenderClass { it.copy(hitPoints = 0) }
                combatMutations.resolveTurnEnd()
            } else {
                combatMutations.addNormalRollMessage(
                    attackerUserId = combatMutations.attackerUserId,
                    defenderUserId = combatMutations.defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = NormalRollMessageType.Damage,
                )
                combatMutations.updateDefenderClass {
                    it.copy(hitPoints = it.hitPoints - diceTotal)
                }
                combatMutations.resolveTurnEnd()
            }
        }
    }

    private fun CombatMutations.markMirrorImageAsUsed() {
        updateDefenderClass { playerClass ->
            val mirrorImage = playerClass.defensiveAbility as DefensiveAbility.MirrorImage
            playerClass.copy(defensiveAbility = mirrorImage.copy(usedThisAttack = true))
        }
    }

    private fun CombatMutations.getDefenderHp(): Int =
        updatedPlayerMap.getValue(defenderUserId).characterClass.hitPoints

    private fun CombatMutations.addNormalRollMessage(
        attackerUserId: String,
        defenderUserId: String,
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
