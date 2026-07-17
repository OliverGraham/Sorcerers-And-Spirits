package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction.Passive.MirrorImage.MirrorImageDie
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.hasPendingExtraAttack
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.shouldMessageDoublesAfterDamage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.shouldMessageDoublesAfterKnockout
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.domain.game.MirrorImageRerollSelection
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.effective

internal class MirrorImageHandler(private val combatMutator: CombatMutator) {

    fun triggerMirrorImage(
        firstDie: Int,
        secondDie: Int,
        attackType: NormalAttackType,
        attackerDoublesBonus: Int,
        mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
    ) =
        combatMutator.triggerMirrorImage(
            firstDie = firstDie,
            secondDie = secondDie,
            attackType = attackType,
            attackerDoublesBonus = attackerDoublesBonus,
            mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
        )

    fun temptFate(mirrorImageAction: AbilityAction.Passive.MirrorImage) {
        val rerolledDiePosition = mirrorImageAction.mirrorImageRerollSelection.toDiePosition()

        if (rerolledDiePosition != null) {
            combatMutator.handleReroll(
                mirrorImageAction = mirrorImageAction,
                rerolledDiePosition = rerolledDiePosition,
            )
        } else {
            combatMutator.handlePass(mirrorImageAction = mirrorImageAction)
        }
    }

    private fun MirrorImageRerollSelection.toDiePosition() =
        when (this) {
            MirrorImageRerollSelection.First -> DiePosition.First
            MirrorImageRerollSelection.Second -> DiePosition.Second
            MirrorImageRerollSelection.None -> null
        }

    private fun CombatMutator.triggerMirrorImage(
        firstDie: Int,
        secondDie: Int,
        attackType: NormalAttackType,
        attackerDoublesBonus: Int,
        mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
    ) {
        addMessage(
            message =
                ArenaMessage.MirrorImage.Trigger(
                    illusionistUserId = defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                )
        )

        val purpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = MirrorImageDie(originalRoll = firstDie),
                secondDie = MirrorImageDie(originalRoll = secondDie),
                attackType = attackType,
                attackerDoublesBonus = attackerDoublesBonus,
                mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
            )

        setNextAbilityRoll(purpose = purpose, activeUserIds = setOf(element = defenderUserId))
    }

    private fun CombatMutator.handleReroll(
        mirrorImageAction: AbilityAction.Passive.MirrorImage,
        rerolledDiePosition: DiePosition,
    ) {
        updateDefenderClass { defender ->
            defender.copy(
                defensiveAbility =
                    mirrorImageAction.mirrorImageDefensiveAbility.copy(
                        usedThisAttack = true,
                        rerolledDiePosition = rerolledDiePosition,
                    )
            )
        }

        val (rerolledMirrorImageAction, rerolledDie) =
            getRerolledMirrorImageAction(
                mirrorImageAction = mirrorImageAction,
                rerolledDiePosition = rerolledDiePosition,
            )

        resolveModifiedRoll(
            mirrorImageAction = rerolledMirrorImageAction,
            isKeptRollDoubles = false,
        ) { messageType ->
            ArenaMessage.MirrorImage.TemptFate(
                illusionistUserId = defenderUserId,
                rerolledMirrorImageDie = rerolledDie,
                attackerUserId = attackerUserId,
                messageType = messageType,
            )
        }
    }

    private fun CombatMutator.getRerolledMirrorImageAction(
        mirrorImageAction: AbilityAction.Passive.MirrorImage,
        rerolledDiePosition: DiePosition,
    ): Pair<AbilityAction.Passive.MirrorImage, MirrorImageDie> {
        val rolledDie =
            diceRoller.roll1D6(userId = defenderUserId, diePosition = rerolledDiePosition).die

        return when (rerolledDiePosition) {
            DiePosition.First -> {
                val updatedFirstDie = mirrorImageAction.firstDie.copy(reroll = rolledDie)

                Pair(
                    first = mirrorImageAction.copy(firstDie = updatedFirstDie),
                    second = updatedFirstDie,
                )
            }

            DiePosition.Second -> {
                val updatedSecondDie = mirrorImageAction.secondDie.copy(reroll = rolledDie)

                Pair(
                    first = mirrorImageAction.copy(secondDie = updatedSecondDie),
                    second = updatedSecondDie,
                )
            }
        }
    }

    private fun CombatMutator.handlePass(mirrorImageAction: AbilityAction.Passive.MirrorImage) {
        resolveModifiedRoll(
            mirrorImageAction = mirrorImageAction,
            isKeptRollDoubles = mirrorImageAction.attackerDoublesBonus > 0,
        ) { messageType ->
            ArenaMessage.MirrorImage.Pass(
                illusionistUserId = defenderUserId,
                attackerUserId = attackerUserId,
                messageType = messageType,
            )
        }
    }

    private fun CombatMutator.resolveModifiedRoll(
        mirrorImageAction: AbilityAction.Passive.MirrorImage,
        isKeptRollDoubles: Boolean,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        val totalRoll =
            mirrorImageAction.firstDie.finalValue +
                mirrorImageAction.secondDie.finalValue +
                mirrorImageAction.attackerDoublesBonus

        when (mirrorImageAction.attackType) {
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
        if (totalRoll >= defender.armorClass) {
            resolveArmorClassHit(isKeptRollDoubles = isKeptRollDoubles, message = message)
        } else {
            resolveArmorClassMiss(isKeptRollDoubles = isKeptRollDoubles, message = message)
        }
    }

    private fun CombatMutator.resolveArmorClassHit(
        isKeptRollDoubles: Boolean,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        addMessage(message = message(NormalRollMessageType.ArmorClassHit))

        if (isKeptRollDoubles) {
            addMessage(message = ArenaMessage.Doubles(userId = attackerUserId))
        }

        continueAttackWithHealthRoll()
    }

    private fun CombatMutator.resolveArmorClassMiss(
        isKeptRollDoubles: Boolean,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        addMessage(message = message(NormalRollMessageType.ArmorClassMiss))

        val shouldMessageDoubles = isKeptRollDoubles && hasPendingExtraAttack()
        if (shouldMessageDoubles) {
            addMessage(message = ArenaMessage.Doubles(userId = attackerUserId))
        }

        resolveTurnEnd()
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
            KillingMomentumHandler(combatMutator = this).triggerKillingMomentum()
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
