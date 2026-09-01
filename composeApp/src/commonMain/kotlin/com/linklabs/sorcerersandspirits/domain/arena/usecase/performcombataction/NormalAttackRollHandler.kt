package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.DodgeHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.EncoreHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.HolyShieldHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.KillingMomentumHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.LuckyCoinHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.MirrorImageHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.PersuadeHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.RiposteHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.SpellPotencyHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TeleportHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.effective

internal class NormalAttackRollHandler(
    private val firstDie: Int,
    private val secondDie: Int,
    private val bonus: Int,
) {
    private val diceTotal = firstDie + secondDie + bonus

    fun handleRoll(currentAction: CurrentCombatAction.NormalAttack, combatMutator: CombatMutator) {
        combatMutator.handleRoll(currentAction = currentAction)
    }

    private fun CombatMutator.handleRoll(currentAction: CurrentCombatAction.NormalAttack) {
        val isTurnOwnersOpeningRoll =
            currentAction.roll == NormalAttackType.ArmorClass &&
                attackerUserId == originalTurnUserId
        if (isTurnOwnersOpeningRoll) spendTurnOwnerAttack()

        val isSpellPotencyRoll = handleSpellPotencyIfApplicable()
        if (isSpellPotencyRoll) return

        when (currentAction.roll) {
            NormalAttackType.ArmorClass -> handleArmorClassRoll()
            NormalAttackType.Health -> handleHealthRoll()
        }
    }

    private fun CombatMutator.handleSpellPotencyIfApplicable(): Boolean {
        val isSpellPotencyRoll =
            attacker.offensiveAbility.effective is OffensiveAbility.SpellPotency &&
                firstDie == secondDie

        if (isSpellPotencyRoll) {
            SpellPotencyHandler(
                combatMutator = this,
                firstDie = firstDie,
                secondDie = secondDie,
                diceTotal = diceTotal,
            )
                .handleSpellPotency()
        }

        return isSpellPotencyRoll
    }

    private fun CombatMutator.handleArmorClassRoll() = if (diceTotal >= defender.armorClass) {
        addNormalRoll(
            type = NormalRollMessageType.ArmorClassHit,
            shouldMessageDoubles = shouldMessageDoublesAfterArmorClassHit(),
        )
        resolveArmorClassHit()
    } else {
        addNormalRoll(
            type = NormalRollMessageType.ArmorClassMiss,
            shouldMessageDoubles = hasPendingExtraAttack(),
        )
        resolveArmorClassMiss()
    }

    private fun CombatMutator.handleHealthRoll() {
        val mirrorImage =
            MirrorImageHandler.getUnusedMirrorImageAbility(ability = defender.defensiveAbility)
        if (mirrorImage != null) {
            addAttackMessage(normalRollMessageType = NormalRollMessageType.Damage)
            MirrorImageHandler(combatMutator = this)
                .triggerMirrorImage(
                    firstDie = firstDie,
                    secondDie = secondDie,
                    attackType = NormalAttackType.Health,
                    existingDoublesBonus = bonus,
                    mirrorImageDefensiveAbility = mirrorImage,
                )
            return
        }

        updateDefenderClass { defenderClass -> defenderClass.takeDamage(damage = diceTotal) }

        return if (!defender.isConscious()) {
            addNormalRoll(
                type = knockoutMessageType(),
                shouldMessageDoubles = shouldMessageDoublesAfterKnockout(),
            )
            resolveKnockout()
        } else {
            addNormalRoll(
                type = NormalRollMessageType.Damage,
                shouldMessageDoubles = shouldMessageDoublesAfterDamage(),
            )
            resolveDamage()
        }
    }

    private fun CombatMutator.knockoutMessageType(): NormalRollMessageType = if (
        HolyShieldHandler.isHolyShieldAvailable(
            defenderDefensiveAbility = defender.defensiveAbility,
        )
    ) {
        NormalRollMessageType.Damage
    } else {
        NormalRollMessageType.Knockout
    }

    private fun CombatMutator.resolveKnockout() {
        val wasHolyShieldTriggered =
            HolyShieldHandler.triggerHolyShieldIfApplicable(
                combatMutator = this,
                defenderDefensiveAbility = defender.defensiveAbility,
            )
        if (wasHolyShieldTriggered) return

        if (consciousPlayers.size <= 1) {
            resolveTurnEnd()
            return
        }

        val attackerOffensiveAbility = attacker.offensiveAbility.effective
        if (attackerOffensiveAbility == OffensiveAbility.KillingMomentum) {
            KillingMomentumHandler(combatMutator = this).triggerKillingMomentum()
            return
        }

        val wasEncoreTriggered =
            EncoreHandler.triggerEncoreIfApplicable(
                combatMutator = this,
                offensiveAbility = attackerOffensiveAbility,
            )
        if (wasEncoreTriggered) return

        resolveTurnEnd()
    }

    private fun CombatMutator.resolveDamage() {
        if (consciousPlayers.size <= 1) {
            resolveTurnEnd()
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

    /**
     * This is when an attacker first attacks a class with a relevant defensive ability, and so
     * control goes to the defender to roll or handle their ability
     */
    private fun CombatMutator.resolveArmorClassHit() {
        when (defender.defensiveAbility) {
            is DefensiveAbility.Teleport -> TeleportHandler(combatMutator = this).triggerTeleport()

            is DefensiveAbility.Riposte ->
                RiposteHandler(combatMutator = this, attackerRollTotal = diceTotal).triggerRiposte()

            is DefensiveAbility.Persuade -> PersuadeHandler(combatMutator = this).triggerPersuade()

            is DefensiveAbility.LuckyCoin ->
                LuckyCoinHandler(combatMutator = this, attackerRollTotal = diceTotal)
                    .triggerLuckyCoin()

            is DefensiveAbility.MirrorImage -> {
                val wasMirrorImageTriggered =
                    triggerMirrorImageIfApplicable(attackType = NormalAttackType.ArmorClass)
                if (wasMirrorImageTriggered) return

                continueAttackWithHealthRoll()
            }

            is DefensiveAbility.Dodge,
            is DefensiveAbility.HolyShield,
            is DefensiveAbility.None,
            -> continueAttackWithHealthRoll()
        }
    }

    /**
     * [DefensiveAbility.Dodge] currently is the only ability that triggers on an ArmorClass miss,
     * so is checked and handled before ending the turn.
     */
    private fun CombatMutator.resolveArmorClassMiss() {
        val defensiveAbility = defender.defensiveAbility
        if (defensiveAbility is DefensiveAbility.Dodge) {
            DodgeHandler(combatMutator = this, dodge = defensiveAbility).triggerDodge()
        }

        resolveTurnEnd()
    }

    private fun CombatMutator.addNormalRoll(
        type: NormalRollMessageType,
        shouldMessageDoubles: Boolean,
    ) {
        addAttackMessage(normalRollMessageType = type)

        addMessage(
            message =
            ArenaMessage.NormalRoll.Outcome(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                attackerClassName = attacker.className,
                attackerClassType = attacker.type,
                defenderClassName = defender.className,
                firstDie = firstDie,
                secondDie = secondDie,
                bonus = bonus,
                type = type,
            ),
        )

        val isDoubles = firstDie == secondDie
        val isDoublesBonusEarned = isDoubles && shouldMessageDoubles
        if (isDoublesBonusEarned) {
            addMessage(message = ArenaMessage.Doubles(className = attacker.className))
        }
    }

    private fun CombatMutator.addAttackMessage(normalRollMessageType: NormalRollMessageType) {
        val attackingMinion = attackingMinionOrNull()
        addMessage(
            message =
            ArenaMessage.NormalRoll.Attack(
                attackerUserId = attackerUserId,
                defenderUserId = defenderUserId,
                attackerClassType = attacker.type,
                impersonatedCharacterType = getImpersonatedCharacterTypeOrNone(),
                normalRollMessageType = normalRollMessageType,
                minionAttackerIndex = attackingMinion?.index,
                minionAttackerType = attackingMinion?.type,
            ),
        )
    }

    private fun CombatMutator.triggerMirrorImageIfApplicable(
        attackType: NormalAttackType,
    ): Boolean {
        val mirrorImage =
            MirrorImageHandler.getUnusedMirrorImageAbility(ability = defender.defensiveAbility)
                ?: return false

        MirrorImageHandler(combatMutator = this)
            .triggerMirrorImage(
                firstDie = firstDie,
                secondDie = secondDie,
                attackType = attackType,
                existingDoublesBonus = bonus,
                mirrorImageDefensiveAbility = mirrorImage,
            )

        return true
    }

    private fun CombatMutator.getImpersonatedCharacterTypeOrNone(): CharacterClassType {
        val offensiveAbility = attacker.offensiveAbility
        if (offensiveAbility !is OffensiveAbility.Impersonate) return CharacterClassType.None

        return offensiveAbility.impersonatedClassType
    }

    companion object {
        fun handleNormalRoll(
            currentAction: CurrentCombatAction.NormalAttack,
            combatMutator: CombatMutator,
        ) {
            val result = combatMutator.diceRoller.roll2D6(userId = combatMutator.attackerUserId)

            val normalAttackRollHandler =
                NormalAttackRollHandler(
                    firstDie = result.firstDie,
                    secondDie = result.secondDie,
                    bonus = result.bonus,
                )

            normalAttackRollHandler.handleRoll(
                currentAction = currentAction,
                combatMutator = combatMutator,
            )
        }
    }
}
