package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.EncoreHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.HolyShieldHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.LuckyCoinHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.MirrorImageHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.PersuadeHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.RiposteHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.SpellPotencyRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TeleportHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TriggerDodge
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TriggerKillingMomentum
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class NormalAttackRollHandler(
    private val firstDie: Int,
    private val secondDie: Int,
    bonus: Int,
) {
    private val diceTotal = firstDie + secondDie + bonus

    fun handleRoll(currentAction: CurrentCombatAction.NormalAttack, combatMutator: CombatMutator) {
        combatMutator.handleRoll(currentAction = currentAction)
    }

    private fun CombatMutator.handleRoll(currentAction: CurrentCombatAction.NormalAttack) {
        val isSpellPotencyRoll = handleSpellPotencyIfApplicable()
        if (isSpellPotencyRoll) return

        when (currentAction.roll) {
            NormalAttackType.ArmorClass -> handleArmorClassRoll()
            NormalAttackType.Health -> handleHealthRoll()
        }
    }

    private fun CombatMutator.handleSpellPotencyIfApplicable(): Boolean {
        val isSpellPotencyRoll =
            attacker.offensiveAbility is OffensiveAbility.SpellPotency && firstDie == secondDie

        if (isSpellPotencyRoll) {
            SpellPotencyRoll(combatMutator = this, diceTotal = diceTotal).invoke()
        }

        return isSpellPotencyRoll
    }

    private fun CombatMutator.handleArmorClassRoll() =
        if (diceTotal >= defender.armorClass) {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassHit)
            resolveArmorClassHit()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassMiss)
            resolveArmorClassMiss()
        }

    private fun CombatMutator.handleHealthRoll() {
        val wasMirrorImageTriggered =
            triggerMirrorImageIfApplicable(attackType = NormalAttackType.Health)
        if (wasMirrorImageTriggered) return

        updateDefenderClass { defenderClass ->
            defenderClass.copy(hitPoints = defenderClass.hitPoints - diceTotal)
        }

        return if (diceTotal >= defender.hitPoints) {
            addNormalRollMessage(type = NormalRollMessageType.Knockout)
            resolveKnockout()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.Damage)
            resolveDamage()
        }
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

        if (attacker.offensiveAbility == OffensiveAbility.KillingMomentum) {
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

    private fun CombatMutator.resolveDamage() {
        if (consciousPlayers.size <= 1) {
            resolveTurnEnd()
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
            is DefensiveAbility.None -> continueAttackWithHealthRoll()
        }
    }

    /**
     * [DefensiveAbility.Dodge] currently is the only ability that triggers on an ArmorClass miss,
     * so is checked and handled before ending the turn.
     */
    private fun CombatMutator.resolveArmorClassMiss() {
        val defensiveAbility = defender.defensiveAbility
        if (defensiveAbility is DefensiveAbility.Dodge) {
            TriggerDodge(combatMutator = this, dodge = defensiveAbility).invoke()
        }

        resolveTurnEnd()
    }

    private fun CombatMutator.addNormalRollMessage(type: NormalRollMessageType) {
        addMessage(
            message =
                ArenaMessage.NormalRoll(
                    attackerUserId = attackerUserId,
                    defenderUserId = defenderUserId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = type,
                )
        )
    }

    private fun CombatMutator.triggerMirrorImageIfApplicable(
        attackType: NormalAttackType
    ): Boolean {
        val mirrorImage =
            MirrorImageHandler.getUnusedMirrorImageAbility(ability = defender.defensiveAbility)
                ?: return false

        MirrorImageHandler(combatMutator = this)
            .triggerMirrorImage(
                firstDie = firstDie,
                secondDie = secondDie,
                attackType = attackType,
                mirrorImageDefensiveAbility = mirrorImage,
            )

        return true
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
