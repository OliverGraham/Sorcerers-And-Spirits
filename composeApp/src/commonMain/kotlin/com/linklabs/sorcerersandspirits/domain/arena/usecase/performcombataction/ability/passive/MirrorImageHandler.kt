package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.MirrorImageDie
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

internal class MirrorImageHandler(private val combatMutator: CombatMutator) {

    fun triggerMirrorImage(
        firstDie: Int,
        secondDie: Int,
        attackType: NormalAttackType,
        mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
    ) =
        combatMutator.triggerMirrorImage(
            firstDie = firstDie,
            secondDie = secondDie,
            attackType = attackType,
            mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
        )

    fun temptFate(mirrorImage: AbilityRollPurpose.Passive.MirrorImage) {
        if (mirrorImage.isReroll) {
            combatMutator.handleReroll(mirrorImage = mirrorImage)
        } else {
            combatMutator.handlePass(mirrorImage = mirrorImage)
        }
    }

    private fun CombatMutator.triggerMirrorImage(
        firstDie: Int,
        secondDie: Int,
        attackType: NormalAttackType,
        mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
    ) {
        addMessage(message = OutgoingArenaMessage.MirrorImage.Trigger)

        val purpose =
            AbilityRollPurpose.Passive.MirrorImage(
                firstDie = MirrorImageDie(originalRoll = firstDie),
                secondDie = MirrorImageDie(originalRoll = secondDie),
                attackType = attackType,
                mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
            )

        setNextCombatAction(nextAction = NextCombatAction.AbilityRoll(purpose = purpose))
    }

    private fun CombatMutator.handleReroll(mirrorImage: AbilityRollPurpose.Passive.MirrorImage) {
        updateDefenderClass { defender ->
            defender.copy(
                defensiveAbility =
                    mirrorImage.mirrorImageDefensiveAbility.copy(usedThisAttack = true)
            )
        }

        val (rerolledDie, originalDie) =
            mirrorImage.run {
                if (firstDie.isRerolled) {
                    Pair(first = firstDie, second = secondDie)
                } else {
                    Pair(first = secondDie, second = firstDie)
                }
            }

        // TODO MirrorImage - message could be combined with more properties to have a single
        //  message for the "Illusionist tempts fate, rerolls a die and {AC outcome || HP outcome}"
        addMessage(
            message =
                OutgoingArenaMessage.MirrorImage.TemptFate(rerolledMirrorImageDie = rerolledDie)
        )

        val totalRoll = rerolledDie.finalValue + originalDie.finalValue
        when (mirrorImage.attackType) {
            NormalAttackType.ArmorClass -> handleArmorClassAttack(totalRoll = totalRoll)
            NormalAttackType.Health -> handleHealthAttack(totalRoll = totalRoll)
        }
    }

    // TODO MirrorImage - messages
    private fun CombatMutator.handleArmorClassAttack(totalRoll: Int) {
        if (totalRoll >= defender.armorClass) {
            val nextRoll =
                NormalAttackRoll.Health(
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                )
            setNextCombatAction(nextAction = NextCombatAction.NormalAttack(roll = nextRoll))
        } else {
            resolveTurnEnd()
        }
    }

    private fun CombatMutator.handleHealthAttack(totalRoll: Int) {
        updateDefenderClass { defender.copy(hitPoints = defender.hitPoints - totalRoll) }

        if (defender.isConscious()) {
            // Ill is alive - Encore
        } else {
            // Ill is dead - Encore, KM
        }
    }

    private fun CombatMutator.handlePass(mirrorImage: AbilityRollPurpose.Passive.MirrorImage) {
        // TODO message and continue attack

        val totalRoll = mirrorImage.firstDie.finalValue + mirrorImage.secondDie.finalValue
        when (mirrorImage.attackType) {
            NormalAttackType.ArmorClass -> handleArmorClassAttack(totalRoll = totalRoll)
            NormalAttackType.Health -> handleHealthAttack(totalRoll = totalRoll)
        }
    }

    companion object {
        fun getUnusedMirrorImageAbility(ability: DefensiveAbility): DefensiveAbility.MirrorImage? {
            val mirrorImage = ability as? DefensiveAbility.MirrorImage ?: return null

            return if (mirrorImage.usedThisAttack) null else mirrorImage
        }
    }
}
