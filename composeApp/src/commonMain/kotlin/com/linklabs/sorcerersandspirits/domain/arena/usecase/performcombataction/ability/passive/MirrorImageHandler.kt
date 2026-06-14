package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction.Passive.MirrorImage.MirrorImageDie
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

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

    fun temptFate(mirrorImage: AbilityAction.Passive.MirrorImage) {
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
        addMessage(message = ArenaMessage.MirrorImage.Trigger)

        val purpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = MirrorImageDie(originalRoll = firstDie),
                secondDie = MirrorImageDie(originalRoll = secondDie),
                attackType = attackType,
                mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
            )

        setNextAbilityRoll(purpose = purpose, activeUserIds = setOf(element = defenderUserId))
    }

    private fun CombatMutator.handleReroll(mirrorImage: AbilityAction.Passive.MirrorImage) {
        updateDefenderClass { defender ->
            defender.copy(
                defensiveAbility =
                    mirrorImage.mirrorImageDefensiveAbility.copy(usedThisAttack = true)
            )
        }

        val rerolledDie =
            if (mirrorImage.firstDie.isRerolled) mirrorImage.firstDie else mirrorImage.secondDie

        resolveModifiedRoll(mirrorImage = mirrorImage) { outcome ->
            ArenaMessage.MirrorImage.TemptFate(
                rerolledMirrorImageDie = rerolledDie,
                attackerUserId = attackerUserId,
                outcome = outcome,
            )
        }
    }

    private fun CombatMutator.handlePass(mirrorImage: AbilityAction.Passive.MirrorImage) {
        resolveModifiedRoll(mirrorImage = mirrorImage) { outcome ->
            ArenaMessage.MirrorImage.Pass(attackerUserId = attackerUserId, outcome = outcome)
        }
    }

    /**
     * Resolves the (possibly rerolled) attack against the defending Illusionist, emitting the
     * supplied message with the resulting [NormalRollMessageType] before applying the outcome's
     * side effects.
     */
    private fun CombatMutator.resolveModifiedRoll(
        mirrorImage: AbilityAction.Passive.MirrorImage,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        val totalRoll = mirrorImage.firstDie.finalValue + mirrorImage.secondDie.finalValue

        when (mirrorImage.attackType) {
            NormalAttackType.ArmorClass ->
                resolveArmorClass(totalRoll = totalRoll, message = message)

            NormalAttackType.Health -> resolveHealth(totalRoll = totalRoll, message = message)
        }
    }

    private fun CombatMutator.resolveArmorClass(
        totalRoll: Int,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        val brokeArmor = totalRoll >= defender.armorClass

        if (brokeArmor) {
            addMessage(message = message(NormalRollMessageType.ArmorClassHit))
            continueAttackWithHealthRoll()
        } else {
            addMessage(message = message(NormalRollMessageType.ArmorClassMiss))
            resolveTurnEnd()
        }
    }

    private fun CombatMutator.resolveHealth(
        totalRoll: Int,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        updateDefenderClass { defender.copy(hitPoints = defender.hitPoints - totalRoll) }

        val isKnockout = !defender.isConscious()
        addMessage(
            message =
                message(
                    if (isKnockout) NormalRollMessageType.Knockout else NormalRollMessageType.Damage
                )
        )

        if (isKnockout && attacker.offensiveAbility == OffensiveAbility.KillingMomentum) {
            TriggerKillingMomentum(combatMutator = this).invoke()
            return
        }

        val wasEncoreTriggered =
            EncoreHandler.triggerEncoreIfApplicable(
                combatMutator = this,
                offensiveAbility = attacker.offensiveAbility,
            )
        if (wasEncoreTriggered) return

        resolveTurnEnd()
    }

    companion object {
        fun getUnusedMirrorImageAbility(ability: DefensiveAbility): DefensiveAbility.MirrorImage? {
            val mirrorImage = ability as? DefensiveAbility.MirrorImage ?: return null

            return if (mirrorImage.usedThisAttack) null else mirrorImage
        }
    }
}
