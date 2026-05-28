package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.DodgeOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.SpellPotencyRoll
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class NormalAttackRoll(
    private val combatMutator: CombatMutator,
    private val firstDie: Int,
    private val secondDie: Int,
    private val attacker: ArenaPlayer.Selected,
    private val defender: ArenaPlayer.Selected,
) {
    private val diceTotal = firstDie + secondDie
    private val attackerOffensiveAbility = attacker.characterClass.offensiveAbility
    private val defenderDefensiveAbility = defender.characterClass.defensiveAbility

    fun handleRoll(currentAction: CurrentCombatAction.NormalAttackRoll): CombatActionUpdates {
        val updatesFromSpellPotency = handleSpellPotencyIfApplicable()
        if (updatesFromSpellPotency != null) return updatesFromSpellPotency

        return when (currentAction) {
            CurrentCombatAction.NormalAttackRoll.ArmorClass -> handleArmorClassRoll()
            CurrentCombatAction.NormalAttackRoll.Health -> handleHealthRoll()
        }
    }

    private fun handleSpellPotencyIfApplicable(): CombatActionUpdates? {
        val isSpellPotencyRoll =
            attackerOffensiveAbility is OffensiveAbility.SpellPotency && firstDie == secondDie

        return if (isSpellPotencyRoll) {
            SpellPotencyRoll(combatMutator = combatMutator, diceTotal = diceTotal).invoke()
        } else {
            null
        }
    }

    private fun handleArmorClassRoll(): CombatActionUpdates =
        if (diceTotal >= defender.characterClass.armorClass) {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassHit)
            resolveArmorClassHit()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassMiss)
            resolveArmorClassMiss()
        }

    private fun handleHealthRoll(): CombatActionUpdates {
        // TODO: Update this MirrorImage handling
        val mirrorImage = defenderDefensiveAbility as? DefensiveAbility.MirrorImage
        val mirrorImageUsed = mirrorImage?.usedThisAttack ?: false

        if (!mirrorImageUsed && mirrorImage != null) {
            combatMutator.addMessage(message = OutgoingArenaMessage.MirrorImage.Trigger)

            return CombatActionUpdates(
                nextActivePlayerUserId = defender.userId,
                nextStep =
                    NextCombatAction.AbilityRoll(
                        purpose =
                            AbilityRollPurpose.Passive.Reaction.MirrorImage(
                                firstDie = firstDie,
                                secondDie = secondDie,
                                triggeredByArmorRoll = false,
                            )
                    ),
            )
        }

        return if (diceTotal >= defender.characterClass.hitPoints) {
            addNormalRollMessage(type = NormalRollMessageType.Knockout)
            resolveKnockout()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.Damage)
            resolveDamage()
        }
    }

    // TODO: Update this, come on

    private fun resolveKnockout(): CombatActionUpdates {
        if (
            defenderDefensiveAbility is DefensiveAbility.HolyShield &&
                !defenderDefensiveAbility.usedThisMatch
        ) {
            combatMutator.addMessage(message = OutgoingArenaMessage.HolyShield.Trigger)

            return CombatActionUpdates(
                nextActivePlayerUserId = defender.userId,
                nextStep =
                    NextCombatAction.AbilityRoll(
                        purpose = AbilityRollPurpose.Passive.Reaction.HolyShield
                    ),
            )
        }

        combatMutator.updateDefenderClass { defenderClass ->
            defenderClass.copy(hitPoints = defenderClass.hitPoints - diceTotal)
        }

        if (attackerOffensiveAbility == OffensiveAbility.KillingMomentum) {
            combatMutator.addMessage(
                message =
                    OutgoingArenaMessage.KillingMomentum(
                        rogueUserId = attacker.userId,
                        isTriggered = true,
                    )
            )

            return CombatActionUpdates(
                nextActivePlayerUserId = attacker.userId,
                nextStep = NextCombatAction.NormalAttackRoll.ArmorClass,
            )
        }

        return combatMutator.resolveTurnEnd()
    }

    private fun resolveDamage(): CombatActionUpdates {
        if (attackerOffensiveAbility == OffensiveAbility.Encore) {
            // TODO: Message
            return CombatActionUpdates(
                nextActivePlayerUserId = attacker.userId,
                nextStep =
                    NextCombatAction.AbilityRoll(
                        purpose = AbilityRollPurpose.Passive.Outcome.Encore
                    ),
            )
        }

        return combatMutator.resolveTurnEnd()
    }

    /**
     * This is when an attacker first attacks a class with a relevant defensive ability, and so
     * control goes to the defender to roll or handle their ability
     */
    private fun resolveArmorClassHit(): CombatActionUpdates =
        when (defenderDefensiveAbility) {
            is DefensiveAbility.Teleport -> {
                combatMutator.addMessage(message = OutgoingArenaMessage.Teleport.Trigger)

                CombatActionUpdates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep =
                        NextCombatAction.AbilityRoll(
                            purpose = AbilityRollPurpose.Passive.Reaction.Teleport
                        ),
                )
            }

            is DefensiveAbility.Riposte -> {
                combatMutator.addMessage(message = OutgoingArenaMessage.Riposte.Trigger)

                CombatActionUpdates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep =
                        NextCombatAction.AbilityRoll(
                            purpose = AbilityRollPurpose.Passive.Reaction.Riposte
                        ),
                )
            }

            is DefensiveAbility.Persuade -> {
                combatMutator.addMessage(message = OutgoingArenaMessage.Persuade.Trigger)

                CombatActionUpdates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep =
                        NextCombatAction.AbilityRoll(
                            purpose = AbilityRollPurpose.Passive.Reaction.Persuade
                        ),
                )
            }

            is DefensiveAbility.LuckyCoin -> {
                combatMutator.addMessage(message = OutgoingArenaMessage.LuckyCoin.Trigger)

                CombatActionUpdates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep =
                        NextCombatAction.AbilityRoll(
                            purpose = AbilityRollPurpose.Passive.Reaction.LuckyCoin
                        ),
                )
            }

            is DefensiveAbility.MirrorImage -> {
                combatMutator.addMessage(message = OutgoingArenaMessage.MirrorImage.Trigger)

                CombatActionUpdates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep =
                        NextCombatAction.AbilityRoll(
                            purpose =
                                AbilityRollPurpose.Passive.Reaction.MirrorImage(
                                    firstDie = firstDie,
                                    secondDie = secondDie,
                                    triggeredByArmorRoll = true,
                                )
                        ),
                )
            }

            is DefensiveAbility.Dodge,
            is DefensiveAbility.HolyShield,
            is DefensiveAbility.None ->
                CombatActionUpdates(
                    nextActivePlayerUserId = attacker.userId,
                    nextStep = NextCombatAction.NormalAttackRoll.Health,
                )
        }

    /**
     * [DefensiveAbility.Dodge] currently is the only ability that triggers on an ArmorClass miss,
     * so is checked and handled before ending the turn.
     */
    private fun resolveArmorClassMiss(): CombatActionUpdates {
        if (defenderDefensiveAbility is DefensiveAbility.Dodge) {
            DodgeOutcome(combatMutator = combatMutator, dodge = defenderDefensiveAbility)
                .applyOutcome()
        }

        return combatMutator.resolveTurnEnd()
    }

    private fun addNormalRollMessage(type: NormalRollMessageType) {
        combatMutator.addMessage(
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
