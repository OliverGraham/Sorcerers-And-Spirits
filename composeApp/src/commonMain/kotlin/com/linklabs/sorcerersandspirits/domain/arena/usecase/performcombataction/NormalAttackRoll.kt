package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CurrentCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.MirrorImageStatus
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.DodgeOutcome
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.SpellPotencyRoll
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility.Dodge
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

class NormalAttackRoll(
    private val combatMutations: CombatMutations,
    private val firstDie: Int,
    private val secondDie: Int,
    private val attacker: ArenaPlayer.Selected,
    private val defender: ArenaPlayer.Selected,
) {
    private val diceTotal = firstDie + secondDie
    private val attackerOffensiveAbility = attacker.characterClass.offensiveAbility
    private val defenderDefensiveAbility = defender.characterClass.defensiveAbility

    fun handleRoll(currentAction: CurrentCombatAction.NormalAttackRoll): CombatActionUpdates {
        val isSpellPotencyRoll =
            attackerOffensiveAbility is OffensiveAbility.SpellPotency && firstDie == secondDie
        if (isSpellPotencyRoll) {
            return SpellPotencyRoll(combatMutations = combatMutations, diceTotal = diceTotal)
                .invoke()
        }

        return when (currentAction) {
            CurrentCombatAction.NormalAttackRoll.ArmorClassRoll -> handleArmorClassRoll()
            CurrentCombatAction.NormalAttackRoll.HealthRoll -> handleHealthRoll()
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
            combatMutations.addMessage(
                message =
                    OutgoingMessage.MirrorImage(
                        illusionistUserId = defender.userId,
                        status = MirrorImageStatus.Pondering,
                    )
            )

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
            // TODO: Message?
            return CombatActionUpdates(
                nextActivePlayerUserId = defender.userId,
                nextStep =
                    NextCombatAction.AbilityRoll(
                        purpose = AbilityRollPurpose.Passive.Reaction.HolyShield
                    ),
            )
        }

        combatMutations.updateDefenderClass { defenderClass ->
            defenderClass.copy(hitPoints = defenderClass.hitPoints - diceTotal)
        }

        if (attackerOffensiveAbility == OffensiveAbility.KillingMomentum) {
            combatMutations.addMessage(
                message =
                    OutgoingMessage.KillingMomentum(
                        rogueUserId = attacker.userId,
                        isTriggered = true,
                    )
            )

            return CombatActionUpdates(
                nextActivePlayerUserId = attacker.userId,
                nextStep = NextCombatAction.NormalAttackRoll.ArmorClassRoll,
            )
        }

        return combatMutations.resolveTurnEnd()
    }

    private fun resolveDamage(): CombatActionUpdates {
        if (attackerOffensiveAbility == OffensiveAbility.Encore) {
            return CombatActionUpdates(
                nextActivePlayerUserId = attacker.userId,
                nextStep =
                    NextCombatAction.AbilityRoll(
                        purpose = AbilityRollPurpose.Passive.Outcome.Encore
                    ),
            )
        }

        return combatMutations.resolveTurnEnd()
    }

    /**
     * This is when an attacker first attacks a class with a relevant defensive ability, and so
     * control goes to the defender to roll or handle their ability
     */
    private fun resolveArmorClassHit(): CombatActionUpdates =
        when (defenderDefensiveAbility) {
            is DefensiveAbility.Teleport ->
                CombatActionUpdates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep =
                        NextCombatAction.AbilityRoll(
                            purpose = AbilityRollPurpose.Passive.Reaction.Teleport
                        ),
                )

            is DefensiveAbility.Riposte ->
                CombatActionUpdates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep =
                        NextCombatAction.AbilityRoll(
                            purpose = AbilityRollPurpose.Passive.Reaction.Riposte
                        ),
                )

            is DefensiveAbility.Persuade ->
                CombatActionUpdates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep =
                        NextCombatAction.AbilityRoll(
                            purpose = AbilityRollPurpose.Passive.Reaction.Persuade
                        ),
                )

            is DefensiveAbility.LuckyCoin ->
                CombatActionUpdates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep =
                        NextCombatAction.AbilityRoll(
                            purpose = AbilityRollPurpose.Passive.Reaction.LuckyCoin
                        ),
                )

            is DefensiveAbility.MirrorImage -> {
                // TODO: update
                combatMutations.addMessage(
                    message =
                        OutgoingMessage.MirrorImage(
                            illusionistUserId = defender.userId,
                            status = MirrorImageStatus.Pondering,
                        )
                )

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
                    nextStep = NextCombatAction.NormalAttackRoll.HealthRoll,
                )
        }

    /**
     * [DefensiveAbility.Dodge] currently is the only ability that triggers on an ArmorClass miss,
     * so is checked and handled before ending the turn.
     */
    private fun resolveArmorClassMiss(): CombatActionUpdates {
        if (defenderDefensiveAbility is DefensiveAbility.Dodge) {
            DodgeOutcome(combatMutations = combatMutations, dodge = defenderDefensiveAbility)
                .applyOutcome()
        }

        return combatMutations.resolveTurnEnd()
    }

    private fun addNormalRollMessage(type: NormalRollMessageType) {
        combatMutations.addMessage(
            message =
                OutgoingMessage.NormalRoll(
                    attackerUserId = attacker.userId,
                    defenderUserId = defender.userId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = type,
                )
        )
    }
}
