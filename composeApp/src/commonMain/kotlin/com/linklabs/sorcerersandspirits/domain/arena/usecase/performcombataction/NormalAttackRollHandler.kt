package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.EncoreHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.MirrorImageHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.PersuadeHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.RiposteHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.SpellPotencyRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TriggerDodge
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TriggerKillingMomentum
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield.HolyShieldHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.luckycoin.LuckyCoinHandler
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.teleport.TriggerTeleport
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class NormalAttackRollHandler(
    private val firstDie: Int,
    private val secondDie: Int,
    attacker: CharacterClass,
    defender: CharacterClass,
) {
    private val diceTotal = firstDie + secondDie
    private val attackerOffensiveAbility = attacker.offensiveAbility
    private val defenderDefensiveAbility = defender.defensiveAbility

    fun handleRoll(currentAction: CurrentCombatAction.NormalAttack, combatMutator: CombatMutator) {
        combatMutator.handleRoll(currentAction = currentAction)
    }

    private fun CombatMutator.handleRoll(currentAction: CurrentCombatAction.NormalAttack) {
        val isSpellPotencyRoll = handleSpellPotencyIfApplicable()
        if (isSpellPotencyRoll) return

        when (currentAction.roll) {
            is NormalAttackRoll.ArmorClass -> handleArmorClassRoll()
            is NormalAttackRoll.Health -> handleHealthRoll()
        }
    }

    private fun CombatMutator.handleSpellPotencyIfApplicable(): Boolean {
        val isSpellPotencyRoll =
            attackerOffensiveAbility is OffensiveAbility.SpellPotency && firstDie == secondDie

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

        return if (diceTotal >= defender.hitPoints) {
            addNormalRollMessage(type = NormalRollMessageType.Knockout)
            resolveKnockout()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.Damage)
            resolveDamage()
        }
    }

    private fun CombatMutator.resolveKnockout() {
        updateDefenderClass { defenderClass ->
            defenderClass.copy(hitPoints = defenderClass.hitPoints - diceTotal)
        }

        val wasHolyShieldTriggered =
            HolyShieldHandler.triggerHolyShieldIfApplicable(
                combatMutator = this,
                defenderDefensiveAbility = defender.defensiveAbility,
            )
        if (wasHolyShieldTriggered) return

        // TODO KillingMomentum
        if (attackerOffensiveAbility == OffensiveAbility.KillingMomentum) {
            TriggerKillingMomentum(combatMutator = this).invoke()
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
        val wasEncoreTriggered =
            EncoreHandler.triggerEncoreIfApplicable(
                combatMutator = this,
                offensiveAbility = attackerOffensiveAbility,
            )
        if (wasEncoreTriggered) return

        resolveTurnEnd()
    }

    /**
     * This is when an attacker first attacks a class with a relevant defensive ability, and so
     * control goes to the defender to roll or handle their ability
     */
    private fun CombatMutator.resolveArmorClassHit() {
        when (defenderDefensiveAbility) {
            is DefensiveAbility.Teleport -> TriggerTeleport(combatMutator = this).invoke()

            is DefensiveAbility.Riposte ->
                RiposteHandler(combatMutator = this, attackerRollTotal = diceTotal).triggerRiposte()

            is DefensiveAbility.Persuade ->
                PersuadeHandler(combatMutator = this).triggerPersuade(attackerRollTotal = diceTotal)

            is DefensiveAbility.LuckyCoin ->
                LuckyCoinHandler(combatMutator = this, attackerRollTotal = diceTotal)
                    .triggerLuckyCoin()

            is DefensiveAbility.MirrorImage -> {
                val wasMirrorImageTriggered =
                    triggerMirrorImageIfApplicable(attackType = NormalAttackType.ArmorClass)
                if (wasMirrorImageTriggered) return

                setNextActionToHealthAttack()
            }

            is DefensiveAbility.Dodge,
            is DefensiveAbility.HolyShield,
            is DefensiveAbility.None -> setNextActionToHealthAttack()
        }
    }

    private fun CombatMutator.setNextActionToHealthAttack() {
        setNextCombatAction(
            nextAction =
                NextCombatAction.NormalAttack(
                    roll =
                        NormalAttackRoll.Health(
                            attackerUserId = attackerUserId,
                            defenderUserId = defenderUserId,
                        )
                )
        )
    }

    /**
     * [DefensiveAbility.Dodge] currently is the only ability that triggers on an ArmorClass miss,
     * so is checked and handled before ending the turn.
     */
    private fun CombatMutator.resolveArmorClassMiss() {
        if (defenderDefensiveAbility is DefensiveAbility.Dodge) {
            TriggerDodge(combatMutator = this, dodge = defenderDefensiveAbility).invoke()
        }

        resolveTurnEnd()
    }

    private fun CombatMutator.addNormalRollMessage(type: NormalRollMessageType) {
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

    private fun CombatMutator.triggerMirrorImageIfApplicable(
        attackType: NormalAttackType
    ): Boolean {
        val mirrorImage =
            MirrorImageHandler.getUnusedMirrorImageAbility(ability = defenderDefensiveAbility)
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
            attacker: CharacterClass,
            defender: CharacterClass,
            currentAction: CurrentCombatAction.NormalAttack,
            combatMutator: CombatMutator,
        ) {
            val firstDie = roll1D6()
            val secondDie = roll1D6()

            val normalAttackRollHandler =
                NormalAttackRollHandler(
                    firstDie = firstDie,
                    secondDie = secondDie,
                    attacker = attacker,
                    defender = defender,
                )

            normalAttackRollHandler.handleRoll(
                currentAction = currentAction,
                combatMutator = combatMutator,
            )
        }
    }
}
