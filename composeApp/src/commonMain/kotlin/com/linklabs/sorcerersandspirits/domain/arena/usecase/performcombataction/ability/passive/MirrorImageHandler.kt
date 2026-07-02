package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction.Passive.MirrorImage.MirrorImageDie
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.shouldMessageDoublesAfterArmorClassMiss
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.shouldMessageDoublesAfterDamage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.shouldMessageDoublesAfterKnockout
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.effective

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
        addMessage(
            message = ArenaMessage.MirrorImage.Trigger(firstDie = firstDie, secondDie = secondDie)
        )

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

        val rolledMirrorImage = rollSelectedDie(mirrorImage = mirrorImage)
        val rerolledDie =
            if (rolledMirrorImage.firstDie.isRerolled) {
                rolledMirrorImage.firstDie
            } else {
                rolledMirrorImage.secondDie
            }

        resolveModifiedRoll(mirrorImage = rolledMirrorImage, isKeptRollDoubles = false) { outcome ->
            ArenaMessage.MirrorImage.TemptFate(
                rerolledMirrorImageDie = rerolledDie,
                attackerUserId = attackerUserId,
                outcome = outcome,
            )
        }
    }

    /**
     * Rolls a fresh 1d6 for the die the Illusionist chose to reroll. The roll's `-1` adjustment is
     * applied later by [MirrorImageDie.finalValue].
     */
    private fun CombatMutator.rollSelectedDie(
        mirrorImage: AbilityAction.Passive.MirrorImage
    ): AbilityAction.Passive.MirrorImage {
        val rolledDie = diceRoller.roll1D6(userId = defenderUserId).total

        return when (mirrorImage.rerollSelection) {
            AbilityAction.Passive.MirrorImage.RerollSelection.First ->
                mirrorImage.copy(firstDie = mirrorImage.firstDie.copy(reroll = rolledDie))

            AbilityAction.Passive.MirrorImage.RerollSelection.Second ->
                mirrorImage.copy(secondDie = mirrorImage.secondDie.copy(reroll = rolledDie))

            AbilityAction.Passive.MirrorImage.RerollSelection.None -> mirrorImage
        }
    }

    /**
     * A kept doubles roll stays consecutive: the Illusionist never touched the dice, so the
     * attacker's +1 bonus survives and the Doubles message deferred by the roll handler is added
     * here instead.
     */
    private fun CombatMutator.handlePass(mirrorImage: AbilityAction.Passive.MirrorImage) {
        val isKeptRollDoubles =
            mirrorImage.firstDie.originalRoll == mirrorImage.secondDie.originalRoll

        resolveModifiedRoll(mirrorImage = mirrorImage, isKeptRollDoubles = isKeptRollDoubles) {
            outcome ->
            ArenaMessage.MirrorImage.Pass(attackerUserId = attackerUserId, outcome = outcome)
        }
    }

    /**
     * Resolves the (possibly rerolled) attack against the defending Illusionist, emitting the
     * supplied message with the resulting [NormalRollMessageType] before applying the outcome's
     * side effects.
     */
    // TODO(SS-188): carry the attacker's doubles bonus into the re-check; the original roll's total
    //  included it, so a keep can currently flip a decisive-bonus hit back into a miss.
    private fun CombatMutator.resolveModifiedRoll(
        mirrorImage: AbilityAction.Passive.MirrorImage,
        isKeptRollDoubles: Boolean,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        val totalRoll = mirrorImage.firstDie.finalValue + mirrorImage.secondDie.finalValue

        when (mirrorImage.attackType) {
            NormalAttackType.ArmorClass ->
                resolveArmorClass(
                    totalRoll = totalRoll,
                    isKeptRollDoubles = isKeptRollDoubles,
                    message = message,
                )

            NormalAttackType.Health ->
                resolveHealth(
                    totalRoll = totalRoll,
                    isKeptRollDoubles = isKeptRollDoubles,
                    message = message,
                )
        }
    }

    private fun CombatMutator.resolveArmorClass(
        totalRoll: Int,
        isKeptRollDoubles: Boolean,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        val brokeArmor = totalRoll >= defender.armorClass

        if (brokeArmor) {
            addMessage(message = message(NormalRollMessageType.ArmorClassHit))
            if (isKeptRollDoubles) {
                addMessage(message = ArenaMessage.Doubles(userId = attackerUserId))
            }
            continueAttackWithHealthRoll()
        } else {
            addMessage(message = message(NormalRollMessageType.ArmorClassMiss))
            val shouldMessageDoubles =
                isKeptRollDoubles && shouldMessageDoublesAfterArmorClassMiss()
            if (shouldMessageDoubles) {
                addMessage(message = ArenaMessage.Doubles(userId = attackerUserId))
            }
            resolveTurnEnd()
        }
    }

    private fun CombatMutator.resolveHealth(
        totalRoll: Int,
        isKeptRollDoubles: Boolean,
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

        val isDoublesBonusStillLive =
            if (isKnockout) {
                shouldMessageDoublesAfterKnockout()
            } else {
                shouldMessageDoublesAfterDamage()
            }
        val shouldMessageDoubles = isKeptRollDoubles && isDoublesBonusStillLive
        if (shouldMessageDoubles) {
            addMessage(message = ArenaMessage.Doubles(userId = attackerUserId))
        }

        if (consciousPlayers.size <= 1) {
            resolveTurnEnd()
            return
        }

        val isKillingMomentumEarned =
            isKnockout && attacker.offensiveAbility.effective == OffensiveAbility.KillingMomentum
        if (isKillingMomentumEarned) {
            TriggerKillingMomentum(combatMutator = this).invoke()
            return
        }

        val wasEncoreTriggered =
            EncoreHandler.triggerEncoreIfApplicable(
                combatMutator = this,
                offensiveAbility = attacker.offensiveAbility.effective,
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
