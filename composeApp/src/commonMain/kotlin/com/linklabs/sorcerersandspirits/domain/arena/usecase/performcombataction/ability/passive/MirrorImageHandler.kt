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
import com.linklabs.sorcerersandspirits.domain.game.DiePosition
import com.linklabs.sorcerersandspirits.domain.game.MirrorImageRerollSelection
import com.linklabs.sorcerersandspirits.domain.game.ability.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.ability.effective

internal class MirrorImageHandler(private val combatMutator: CombatMutator) {

    fun triggerMirrorImage(
        firstDie: Int,
        secondDie: Int,
        attackType: NormalAttackType,
        existingDoublesBonus: Int,
        mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
    ) = combatMutator.triggerMirrorImage(
        firstDie = firstDie,
        secondDie = secondDie,
        attackType = attackType,
        existingDoublesBonus = existingDoublesBonus,
        mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
    )

    fun temptFate(mirrorImageAction: AbilityAction.Passive.MirrorImage) {
        val rerolledDiePosition = mirrorImageAction.mirrorImageRerollSelection.toDiePosition()

        println(
            "MIRROR IMAGE DEBUG: temptFate() called and  rerolledDiePosition == $rerolledDiePosition",
        )

        if (rerolledDiePosition != null) {
            combatMutator.handleReroll(
                mirrorImageAction = mirrorImageAction,
                rerolledDiePosition = rerolledDiePosition,
            )
        } else {
            combatMutator.handlePass(mirrorImageAction = mirrorImageAction)
        }
    }

    private fun CombatMutator.triggerMirrorImage(
        firstDie: Int,
        secondDie: Int,
        attackType: NormalAttackType,
        existingDoublesBonus: Int,
        mirrorImageDefensiveAbility: DefensiveAbility.MirrorImage,
    ) {
        addMessage(
            message =
            ArenaMessage.MirrorImage.Trigger(
                illusionistUserId = defenderUserId,
                firstDie = firstDie,
                secondDie = secondDie,
            ),
        )

        val purpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = MirrorImageDie(originalRoll = firstDie),
                secondDie = MirrorImageDie(originalRoll = secondDie),
                attackType = attackType,
                existingDoublesBonus = existingDoublesBonus,
                mirrorImageDefensiveAbility = mirrorImageDefensiveAbility,
            )

        setNextAbilityRoll(purpose = purpose, activeUserIds = setOf(element = defenderUserId))
    }

    private fun MirrorImageRerollSelection.toDiePosition() = when (this) {
        MirrorImageRerollSelection.First -> DiePosition.First
        MirrorImageRerollSelection.Second -> DiePosition.Second
        MirrorImageRerollSelection.None -> null
    }

    private fun CombatMutator.handleReroll(
        mirrorImageAction: AbilityAction.Passive.MirrorImage,
        rerolledDiePosition: DiePosition,
    ) {
        updateDefenderClass { defender ->
            defender.copy(
                defensiveAbility =
                mirrorImageAction.mirrorImageDefensiveAbility.copy(usedThisAttack = true),
            )
        }

        val (rerolledMirrorImageAction, rerolledDie) =
            getRerolledMirrorImageAction(
                mirrorImageAction = mirrorImageAction,
                rerolledDiePosition = rerolledDiePosition,
            )

        resolveModifiedRoll(mirrorImageAction = rerolledMirrorImageAction) { messageType ->
            ArenaMessage.MirrorImage.TemptFate(
                illusionistUserId = defenderUserId,
                rerolledMirrorImageDie = rerolledDie,
                attackerUserId = attackerUserId,
                attackerClassName = attacker.className,
                attackerClassType = attacker.type,
                messageType = messageType,
            )
        }
    }

    private fun CombatMutator.getRerolledMirrorImageAction(
        mirrorImageAction: AbilityAction.Passive.MirrorImage,
        rerolledDiePosition: DiePosition,
    ): Pair<AbilityAction.Passive.MirrorImage, MirrorImageDie> {
        val rolledDie =
            diceRoller
                .roll1D6(
                    userId = defenderUserId,
                    diePosition = rerolledDiePosition,
                    isMirrorImageReroll = true,
                    disablesSibling = false,
                    dieColor = dieColorFor(userId = defenderUserId),
                )
                .die

        return when (rerolledDiePosition) {
            DiePosition.First -> {
                val updatedFirstDie = mirrorImageAction.firstDie.copy(reroll = rolledDie)

                Pair(
                    first =
                    mirrorImageAction.copy(
                        firstDie = updatedFirstDie,
                        existingDoublesBonus = 0,
                    ),
                    second = updatedFirstDie,
                )
            }

            DiePosition.Second -> {
                val updatedSecondDie = mirrorImageAction.secondDie.copy(reroll = rolledDie)

                Pair(
                    first =
                    mirrorImageAction.copy(
                        secondDie = updatedSecondDie,
                        existingDoublesBonus = 0,
                    ),
                    second = updatedSecondDie,
                )
            }
        }
    }

    private fun CombatMutator.handlePass(mirrorImageAction: AbilityAction.Passive.MirrorImage) {
        diceRoller.mirrorImagePass(
            firstDie = mirrorImageAction.firstDie.finalValue,
            secondDie = mirrorImageAction.secondDie.finalValue,
        )

        resolveModifiedRoll(mirrorImageAction = mirrorImageAction) { messageType ->
            ArenaMessage.MirrorImage.Pass(
                illusionistUserId = defenderUserId,
                attackerUserId = attackerUserId,
                attackerClassName = attacker.className,
                attackerClassType = attacker.type,
                messageType = messageType,
                baseRoll = mirrorImageAction.baseRoll(),
                doublesBonus = mirrorImageAction.existingDoublesBonus,
            )
        }
    }

    private fun AbilityAction.Passive.MirrorImage.baseRoll() = firstDie.finalValue + secondDie.finalValue

    private fun AbilityAction.Passive.MirrorImage.total() = baseRoll() + existingDoublesBonus

    // TODO: More refactoring. This function, resolveModifiedRoll, is too abstract
    private fun CombatMutator.resolveModifiedRoll(
        mirrorImageAction: AbilityAction.Passive.MirrorImage,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        val totalRoll = mirrorImageAction.total()
        val isAttackerDoublesLive = diceRoller.getDoublesBonus()?.userId == attackerUserId

        when (mirrorImageAction.attackType) {
            NormalAttackType.ArmorClass ->
                resolveArmorClass(
                    totalRoll = totalRoll,
                    isAttackerDoublesLive = isAttackerDoublesLive,
                    message = message,
                )

            NormalAttackType.Health ->
                resolveHealth(
                    totalRoll = totalRoll,
                    isAttackerDoublesLive = isAttackerDoublesLive,
                    message = message,
                )
        }
    }

    private fun CombatMutator.resolveArmorClass(
        totalRoll: Int,
        isAttackerDoublesLive: Boolean,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        if (totalRoll >= defender.armorClass) {
            resolveArmorClassHit(isAttackerDoublesLive = isAttackerDoublesLive, message = message)
        } else {
            resolveArmorClassMiss(isAttackerDoublesLive = isAttackerDoublesLive, message = message)
        }
    }

    private fun CombatMutator.resolveArmorClassHit(
        isAttackerDoublesLive: Boolean,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        addMessage(message = message(NormalRollMessageType.ArmorClassHit))

        if (isAttackerDoublesLive) {
            addMessage(
                message =
                ArenaMessage.Doubles(className = attacker.className, classType = attacker.type),
            )
        }

        continueAttackWithHealthRoll()
    }

    private fun CombatMutator.resolveArmorClassMiss(
        isAttackerDoublesLive: Boolean,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        addMessage(message = message(NormalRollMessageType.ArmorClassMiss))

        val shouldMessageDoubles = isAttackerDoublesLive && hasPendingExtraAttack()
        if (shouldMessageDoubles) {
            addMessage(
                message =
                ArenaMessage.Doubles(className = attacker.className, classType = attacker.type),
            )
        }

        resolveTurnEnd()
    }

    private fun CombatMutator.resolveHealth(
        totalRoll: Int,
        isAttackerDoublesLive: Boolean,
        message: (NormalRollMessageType) -> ArenaMessage,
    ) {
        val isKnockout = wouldBeKnockedOut(damage = totalRoll, userId = defenderUserId)
        addMessage(
            message =
            message(
                if (isKnockout) NormalRollMessageType.Knockout else NormalRollMessageType.Damage,
            ),
        )

        val hadAbsorbingMinions = hasAbsorbingMinions(userId = defenderUserId)
        val damageAfterMinionShield = takeDamage(damage = totalRoll, userId = defenderUserId)
        if (hadAbsorbingMinions && damageAfterMinionShield > 0) {
            addMinionShieldResidualDamage(
                summonerUserId = defenderUserId,
                damageAfterMinionShield = damageAfterMinionShield,
            )
        }

        val isDoublesBonusStillLive =
            if (isKnockout) {
                shouldMessageDoublesAfterKnockout()
            } else {
                shouldMessageDoublesAfterDamage()
            }
        val shouldMessageDoubles = isAttackerDoublesLive && isDoublesBonusStillLive
        if (shouldMessageDoubles) {
            addMessage(
                message =
                ArenaMessage.Doubles(className = attacker.className, classType = attacker.type),
            )
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
