package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.SpellPotencyRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TriggerDodge
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.TriggerKillingMomentum
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.teleport.TriggerTeleport
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class NormalAttackRoll(
    private val firstDie: Int,
    private val secondDie: Int,
    private val attacker: ArenaPlayer.Selected,
    private val defender: ArenaPlayer.Selected,
) {
    private val diceTotal = firstDie + secondDie
    private val attackerOffensiveAbility = attacker.characterClass.offensiveAbility
    private val defenderDefensiveAbility = defender.characterClass.defensiveAbility

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
        if (diceTotal >= defender.characterClass.armorClass) {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassHit)
            resolveArmorClassHit()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassMiss)
            resolveArmorClassMiss()
        }

    private fun CombatMutator.handleHealthRoll() {
        // TODO MirrorImage
        val mirrorImage = defenderDefensiveAbility as? DefensiveAbility.MirrorImage
        val mirrorImageUsed = mirrorImage?.usedThisAttack ?: false

        if (!mirrorImageUsed && mirrorImage != null) {
            addMessage(message = OutgoingArenaMessage.MirrorImage.Trigger)

            // nextActivePlayerUserId = defender.userId,
            NextCombatAction.AbilityRoll(
                purpose =
                    AbilityRollPurpose.Passive.Reaction.MirrorImage(
                        firstDie = firstDie,
                        secondDie = secondDie,
                        triggeredByArmorRoll = false,
                    )
            )
        }

        // TODO Encore
        if (attackerOffensiveAbility == OffensiveAbility.Encore) {
            // TODO: Message

            // nextActivePlayerUserId = attacker.userId,
            NextCombatAction.AbilityRoll(purpose = AbilityRollPurpose.Passive.Outcome.Encore)

            return
        }

        return if (diceTotal >= defender.characterClass.hitPoints) {
            addNormalRollMessage(type = NormalRollMessageType.Knockout)
            resolveKnockout()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.Damage)
            resolveDamage()
        }
    }

    private fun CombatMutator.resolveKnockout() {
        // TODO HolyShield
        if (
            defenderDefensiveAbility is DefensiveAbility.HolyShield &&
                !defenderDefensiveAbility.usedThisMatch
        ) {
            addMessage(message = OutgoingArenaMessage.HolyShield.Trigger)

            // nextActivePlayerUserId = defender.userId,
            NextCombatAction.AbilityRoll(purpose = AbilityRollPurpose.Passive.Reaction.HolyShield)
        }

        updateDefenderClass { defenderClass ->
            defenderClass.copy(hitPoints = defenderClass.hitPoints - diceTotal)
        }

        if (attackerOffensiveAbility == OffensiveAbility.KillingMomentum) {
            TriggerKillingMomentum(combatMutator = this).invoke()
            return
        }

        resolveTurnEnd()
    }

    private fun CombatMutator.resolveDamage() {
        resolveTurnEnd()
    }

    /**
     * This is when an attacker first attacks a class with a relevant defensive ability, and so
     * control goes to the defender to roll or handle their ability
     */
    private fun CombatMutator.resolveArmorClassHit() {
        when (defenderDefensiveAbility) {
            is DefensiveAbility.Teleport -> TriggerTeleport(combatMutator = this).invoke()

            is DefensiveAbility.Riposte -> {
                addMessage(message = OutgoingArenaMessage.Riposte.Trigger)

                // nextActivePlayerUserId = defender.userId,
                // nextStep =
                NextCombatAction.AbilityRoll(purpose = AbilityRollPurpose.Passive.Reaction.Riposte)
            }

            is DefensiveAbility.Persuade -> {
                addMessage(message = OutgoingArenaMessage.Persuade.Trigger)

                // nextActivePlayerUserId = defender.userId,
                // nextStep =
                NextCombatAction.AbilityRoll(purpose = AbilityRollPurpose.Passive.Reaction.Persuade)
            }

            is DefensiveAbility.LuckyCoin -> {
                addMessage(message = OutgoingArenaMessage.LuckyCoin.Trigger)

                // nextActivePlayerUserId = defender.userId,
                // nextStep =
                NextCombatAction.AbilityRoll(
                    purpose = AbilityRollPurpose.Passive.Reaction.LuckyCoin
                )
            }

            is DefensiveAbility.MirrorImage -> {
                // TODO MirrorImage
                addMessage(message = OutgoingArenaMessage.MirrorImage.Trigger)

                // nextActivePlayerUserId = defender.userId,
                // nextStep =
                NextCombatAction.AbilityRoll(
                    purpose =
                        AbilityRollPurpose.Passive.Reaction.MirrorImage(
                            firstDie = firstDie,
                            secondDie = secondDie,
                            triggeredByArmorRoll = true,
                        )
                )
            }

            is DefensiveAbility.Dodge,
            is DefensiveAbility.HolyShield,
            is DefensiveAbility.None ->
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
                    attackerUserId = attacker.userId,
                    defenderUserId = defender.userId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = type,
                )
        )
    }
}
