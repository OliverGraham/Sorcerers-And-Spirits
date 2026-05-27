package com.linklabs.sorcerersandspirits.domain.arena.utilities

import com.linklabs.sorcerersandspirits.domain.arena.model.AllInOutcome
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatStep
import com.linklabs.sorcerersandspirits.domain.arena.model.MirrorImageStatus
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.PersuadeOutcome
import com.linklabs.sorcerersandspirits.domain.arena.model.RollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

data class RollOutcome(
    val messages: List<OutgoingMessage>,
    val updatedPlayerMap: Map<UserId, ArenaPlayer.Selected>,
    val nextActivePlayerUserId: String,
    val nextStep: CombatStep,
)

internal class PerformCombatAction(
    arguments: CombatActionArguments,
    private val currentStep: CombatStep,
) {

    data class Updates(
        val nextActivePlayerUserId: String,
        val nextStep: CombatStep,
        val isNewTurn: Boolean = false,
    )

    private val messages = mutableListOf<OutgoingMessage>()
    private val updatedPlayerMap = arguments.playerMap.toMutableMap()

    private val attacker = arguments.attacker
    val attackerOffensiveAbility = attacker.characterClass.offensiveAbility

    private val defender = arguments.defender
    val defenderDefensiveAbility = defender.characterClass.defensiveAbility

    val roundNumber = arguments.roundNumber

    // TODO: To reduce insanity, move abilities to separate files with some sort update lambda?

    // TODO: if isNewTurn is true, call resolveTurnEnd() before return RollOutcome
    operator fun invoke(): RollOutcome {
        val updates = resolveStep(step = currentStep)

        return RollOutcome(
            messages = messages,
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = updates.nextActivePlayerUserId,
            nextStep = updates.nextStep,
        )
    }

    private fun handleAbilityRoll(purpose: RollPurpose): Updates =
        when (purpose) {
            is RollPurpose.TeleportDefenderEscape -> handleTeleportDefenderEscape()

            is RollPurpose.TeleportAttackerChase ->
                handleTeleportAttackerChase(defenderRoll = purpose.defenderRoll)

            is RollPurpose.RiposteCounter -> handleRiposteCounter()

            is RollPurpose.PersuadeStopOrRedirect -> handlePersuadeStopOrRedirect()

            is RollPurpose.LuckyCoinArmorBonus -> handleLuckyCoinArmorBonus()

            is RollPurpose.LuckyCoinReflect -> handleLuckyCoinReflect()

            is RollPurpose.AllInOutcome -> handleAllInOutcome()

            is RollPurpose.SummonMinionRoll -> handleSummonMinionRoll()

            is RollPurpose.PrepareAttackRoll -> handlePrepareAttackRoll()

            is RollPurpose.EncoreCheck -> handleEncoreCheck()

            is RollPurpose.HolyShieldSurvival -> handleHolyShieldSurvival()

            is RollPurpose.HolyShieldRetaliation -> handleHolyShieldRetaliation()

            is RollPurpose.MirrorImageChoice -> handleMirrorImageChoice(purpose = purpose)

            is RollPurpose.MirrorImagePass -> handleMirrorImagePass(purpose = purpose)

            is RollPurpose.MirrorImageReroll -> handleMirrorImageReroll(purpose = purpose)

            is RollPurpose.ImpersonateChoice -> handleImpersonateChoice()
        }

    private fun handleMirrorImageChoice(purpose: RollPurpose.MirrorImageChoice): Updates {
        // This is a decision step where the Illusionist chooses whether to reroll a die or pass.
        // Returning the same state allows the UI to display the choice and wait for user input.
        // To exit this state, the UI should call invoke with either MirrorImagePass or
        // MirrorImageReroll.
        // TODO: Message
        return Updates(
            nextActivePlayerUserId = defender.userId,
            nextStep = CombatStep.AbilityRoll(purpose = purpose),
        )
    }

    private fun handleMirrorImagePass(purpose: RollPurpose.MirrorImagePass): Updates {
        // If the Illusionist chooses not to reroll, the hit/damage stands and combat resumes.
        // TODO: Messages
        messages.add(
            element =
                OutgoingMessage.MirrorImage(
                    illusionistUserId = defender.userId,
                    status = MirrorImageStatus.Passed,
                )
        )

        return if (purpose.triggeredByArmorRoll) {
            // Hit stands, move to damage roll
            Updates(nextActivePlayerUserId = attacker.userId, nextStep = CombatStep.HealthRoll)
        } else {
            // Damage roll stands, finalize resolution using original dice
            val dice = Dice(firstDie = purpose.firstDie, secondDie = purpose.secondDie)
            dice.resumeHealthRollResolution()
        }
    }

    private fun handleMirrorImageReroll(purpose: RollPurpose.MirrorImageReroll): Updates {
        val roll = roll1D6() - 1

        val newFirstDie = if (purpose.rerollFirstDie) roll else purpose.firstDie
        val newSecondDie = if (!purpose.rerollFirstDie) roll else purpose.secondDie

        messages.add(
            element =
                OutgoingMessage.MirrorImage(
                    illusionistUserId = defender.userId,
                    status = MirrorImageStatus.Rerolled,
                )
        )

        val dice = Dice(firstDie = newFirstDie, secondDie = newSecondDie)

        return if (purpose.triggeredByArmorRoll) {
            dice.resolveAfterMirrorImageArmorRoll()
        } else {
            dice.resolveAfterMirrorImageHealthRoll()
        }
    }

    private fun handleImpersonateChoice(): Updates {
        // Implementation for Impersonate choice
        // For now, let's just transition to the next step (e.g. Resolution or AC Roll)
        // TODO: Message
        return Updates(nextActivePlayerUserId = attacker.userId, nextStep = CombatStep.Resolution)
    }

    private fun handleTeleportDefenderEscape(): Updates {
        val roll = roll1D6()
        // TODO: Message
        return Updates(
            nextActivePlayerUserId = attacker.userId,
            nextStep =
                CombatStep.AbilityRoll(
                    purpose = RollPurpose.TeleportAttackerChase(defenderRoll = roll)
                ),
        )
    }

    private fun handleTeleportAttackerChase(defenderRoll: Int): Updates {
        val attackerRoll = roll1D6()
        val escaped = defenderRoll > attackerRoll

        messages.add(
            element =
                OutgoingMessage.Teleport(
                    mageUserId = defender.userId,
                    attackerUserId = attacker.userId,
                    escaped = escaped,
                )
        )

        return if (escaped) {
            resolveTurnEnd()
        } else {
            Updates(nextActivePlayerUserId = attacker.userId, nextStep = CombatStep.HealthRoll)
        }
    }

    private fun handleRiposteCounter(): Updates {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie
        val triggered = total in 6..8

        messages.add(
            element =
                OutgoingMessage.Riposte(
                    duelistUserId = defender.userId,
                    attackerUserId = attacker.userId,
                    triggered = triggered,
                )
        )

        return if (triggered) {
            // Flip attack: Defender now attacks Attacker
            Updates(nextActivePlayerUserId = defender.userId, nextStep = CombatStep.ArmorClassRoll)
        } else {
            Updates(nextActivePlayerUserId = attacker.userId, nextStep = CombatStep.HealthRoll)
        }
    }

    private fun handlePersuadeStopOrRedirect(): Updates {
        val roll = roll1D6()
        val outcome =
            when (roll) {
                5 -> PersuadeOutcome.Stopped
                6 -> PersuadeOutcome.Redirected
                else -> PersuadeOutcome.Failed
            }

        messages.add(
            element =
                OutgoingMessage.Persuade(
                    bardUserId = defender.userId,
                    attackerUserId = attacker.userId,
                    outcome = outcome,
                )
        )

        return when (outcome) {
            PersuadeOutcome.Stopped -> resolveTurnEnd()

            PersuadeOutcome.Redirected -> {
                Updates(
                    nextActivePlayerUserId = attacker.userId,
                    nextStep = CombatStep.Resolution, // Attacker needs to pick new target
                )
            }

            PersuadeOutcome.Failed -> {
                Updates(nextActivePlayerUserId = attacker.userId, nextStep = CombatStep.HealthRoll)
            }
        }
    }

    private fun handleLuckyCoinArmorBonus(): Updates {
        // TODO: Fix this logic
        val roll = roll1D6()

        val newAc = defender.characterClass.armorClass + roll
        updatedPlayerMap[defender.userId] =
            defender.copy(characterClass = defender.characterClass.copy(armorClass = newAc))

        if (roll == 6) {
            return Updates(
                nextActivePlayerUserId = defender.userId,
                nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.LuckyCoinReflect),
            )
        }

        messages.add(
            element =
                OutgoingMessage.LuckyCoin(
                    gamblerUserId = defender.userId,
                    attackerUserId = attacker.userId,
                    armorBonus = roll,
                    reflectDamage = 0,
                )
        )

        // Re-evaluate AC roll with new AC
        // This is tricky. For simplicity, let's assume it still hits if the original roll was high
        // enough?
        // Actually, the original roll might now be a miss.
        // We'd need the original roll value here.
        // For now, let's just proceed to Health Roll and let the UI/user handle if it's a miss now.
        // Better: return to AC roll step with the new map?
        return Updates(nextActivePlayerUserId = attacker.userId, nextStep = CombatStep.HealthRoll)
    }

    private fun handleLuckyCoinReflect(): Updates {
        val damage = roll1D6()

        val newHp = (attacker.characterClass.hitPoints - damage)
        updatedPlayerMap[attacker.userId] =
            attacker.copy(characterClass = attacker.characterClass.copy(hitPoints = newHp))

        messages.add(
            element =
                OutgoingMessage.LuckyCoin(
                    gamblerUserId = defender.userId,
                    attackerUserId = attacker.userId,
                    armorBonus = 6, // We know it was a 6 to get here
                    reflectDamage = damage,
                )
        )

        return Updates(nextActivePlayerUserId = attacker.userId, nextStep = CombatStep.HealthRoll)
    }

    private fun handleSummonMinionRoll(): Updates {}

    private fun handleEncoreCheck(): Updates {
        val roll = roll1D6()
        val triggered = roll in 5..6

        messages.add(
            element = OutgoingMessage.Encore(bardUserId = attacker.userId, triggered = triggered)
        )

        return if (triggered) {
            Updates(
                nextActivePlayerUserId = attacker.userId,
                nextStep = CombatStep.Resolution, // Attack again
            )
        } else {
            resolveTurnEnd()
        }
    }

    private fun handleHolyShieldSurvival(): Updates {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie
        val survived = total in 10..12

        val paladin = defender

        return if (survived) {
            updatedPlayerMap[paladin.userId] =
                paladin.copy(
                    characterClass =
                        paladin.characterClass.copy(
                            hitPoints = 1,
                            defensiveAbility =
                                (paladin.characterClass.defensiveAbility
                                        as DefensiveAbility.HolyShield)
                                    .copy(usedThisMatch = true),
                        )
                )
            Updates(
                nextActivePlayerUserId = paladin.userId,
                nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.HolyShieldRetaliation),
            )
        } else {
            updatedPlayerMap[paladin.userId] =
                paladin.copy(characterClass = paladin.characterClass.copy(hitPoints = 0))

            messages.add(
                element =
                    OutgoingMessage.HolyShield(
                        paladinUserId = paladin.userId,
                        survived = false,
                        retaliationDamage = 0,
                    )
            )
            resolveTurnEnd()
        }
    }

    private fun handleHolyShieldRetaliation(): Updates {
        val damage = roll1D6()

        val newHp = (attacker.characterClass.hitPoints - damage)
        updatedPlayerMap[attacker.userId] =
            attacker.copy(characterClass = attacker.characterClass.copy(hitPoints = newHp))

        messages.add(
            element =
                OutgoingMessage.HolyShield(
                    paladinUserId = defender.userId,
                    survived = true,
                    retaliationDamage = damage,
                )
        )

        return resolveTurnEnd()
    }

    private fun handleResolution(): Updates {
        // Usually transitions to AC roll for the attacker
        // TODO: what is really going on here?
        return Updates(
            nextActivePlayerUserId = attacker.userId,
            nextStep = CombatStep.ArmorClassRoll,
        )
    }

    // TODO: Need to change name of this class and figure out the difference between
    //  what's in here vs what's above

    data class Dice(val firstDie: Int, val secondDie: Int) {
        val diceTotal = firstDie + secondDie
    }

    private fun Dice.isSpellPotencyTrigger() =
        attackerOffensiveAbility == OffensiveAbility.SpellPotency && firstDie == secondDie

    fun Dice.handleArmorClassRoll(): Updates {
        if (isSpellPotencyTrigger()) return resolveSpellPotency()

        return if (diceTotal >= defender.characterClass.armorClass) {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassHit)
            resolveArmorClassHit()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassMiss)
            resolveArmorClassMiss()
        }
    }

    // TODO: Update this
    fun Dice.handleHealthRoll(): Updates {
        if (isSpellPotencyTrigger()) return resolveSpellPotency()

        val mirrorImage = defenderDefensiveAbility as? DefensiveAbility.MirrorImage
        val mirrorImageUsed = mirrorImage?.usedThisAttack ?: false

        if (!mirrorImageUsed && mirrorImage != null) {
            messages.add(
                element =
                    OutgoingMessage.MirrorImage(
                        illusionistUserId = defender.userId,
                        status = MirrorImageStatus.Pondering,
                    )
            )
            return Updates(
                nextActivePlayerUserId = defender.userId,
                nextStep =
                    CombatStep.AbilityRoll(
                        purpose =
                            RollPurpose.MirrorImageChoice(
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

    fun Dice.resolveAfterMirrorImageArmorRoll(): Updates {
        // TODO: Messages

        markMirrorImageAsUsed()

        return if (diceTotal >= defender.characterClass.armorClass) {
            // It still hits!
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassHit)
            resolveArmorClassHit()
        } else {
            // It's now a miss!
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassMiss)
            resolveArmorClassMiss()
        }
    }

    fun Dice.resolveAfterMirrorImageHealthRoll(): Updates {
        // TODO: messages

        markMirrorImageAsUsed()

        return resumeHealthRollResolution()
    }

    // TODO: This seems like it's not working? Double check
    private fun markMirrorImageAsUsed() {
        val currentAbility =
            defender.characterClass.defensiveAbility as DefensiveAbility.MirrorImage

        updatedPlayerMap[defender.userId] =
            defender.copy(
                characterClass =
                    defender.characterClass.copy(
                        defensiveAbility = currentAbility.copy(usedThisAttack = true)
                    )
            )
    }

    fun Dice.resumeHealthRollResolution(): Updates =
        if (diceTotal >= defender.characterClass.hitPoints) {
            addNormalRollMessage(type = NormalRollMessageType.Knockout)
            resolveKnockout()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.Damage)
            resolveDamage()
        }
}
