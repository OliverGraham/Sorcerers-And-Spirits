package com.linklabs.sorcerersandspirits.domain.arena.utilities

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatStep
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.RollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

// TODO: Does this still need to be a sealed interface?
sealed interface RollOutcome {
    data class Results(
        val messages: List<OutgoingMessage>,
        val updatedPlayerMap: Map<UserId, ArenaPlayer.Selected>,
        val nextActivePlayerUserId: String,
        val nextStep: CombatStep,
    ) : RollOutcome
}

internal class Attack(
    private val firstDie: Int,
    private val secondDie: Int,
    private val arguments: CombatActionArguments,
) {
    private val messages = mutableListOf<OutgoingMessage>()
    private val updatedPlayerMap = arguments.playerMap.toMutableMap()

    private val diceTotal = firstDie + secondDie

    private data class Updates(val nextActivePlayerUserId: String, val nextStep: CombatStep)

    fun handleArmorClassRoll(): RollOutcome {
        // TODO: return Updates
        val updates = arguments.handleArmorClassRoll()

        // TODO: return Results using class-wide state and Updates
    }

    fun handleHealthRoll(): RollOutcome {
        // TODO: return Updates
        val updates = arguments.handleHealthRoll()

        // TODO: return Results using class-wide state and Updates
    }

    private fun CombatActionArguments.handleArmorClassRoll(): RollOutcome =
        if (diceTotal >= defender.characterClass.armorClass) {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassHit)
            resolveArmorClassHit()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassMiss)
            resolveArmorClassMiss()
        }

    private fun CombatActionArguments.resolveArmorClassHit(): RollOutcome =
        when (defenderDefensiveAbility) {
            is DefensiveAbility.Teleport -> {
                RollOutcome.Results(
                    messages = messages,
                    updatedPlayerMap = updatedPlayerMap,
                    nextActivePlayerUserId = defender.userId,
                    nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.TeleportDefenderEscape),
                )
            }

            is DefensiveAbility.Riposte -> {
                RollOutcome.Results(
                    messages = messages,
                    updatedPlayerMap = updatedPlayerMap,
                    nextActivePlayerUserId = defender.userId,
                    nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.RiposteCounter),
                )
            }

            is DefensiveAbility.Persuade -> {
                RollOutcome.Results(
                    messages = messages,
                    updatedPlayerMap = updatedPlayerMap,
                    nextActivePlayerUserId = defender.userId,
                    nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.PersuadeStopOrRedirect),
                )
            }

            is DefensiveAbility.LuckyCoin -> {
                RollOutcome.Results(
                    messages = messages,
                    updatedPlayerMap = updatedPlayerMap,
                    nextActivePlayerUserId = defender.userId,
                    nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.LuckyCoinArmorBonus),
                )
            }

            is DefensiveAbility.MirrorImage -> {
                // TODO: This logic needs to be updated - This defensive ability can be used an
                //  unlimited number of times
                if (!defenderDefensiveAbility.usedThisRound) {
                    RollOutcome.Results(
                        messages = messages,
                        updatedPlayerMap = updatedPlayerMap,
                        nextActivePlayerUserId = defender.userId,
                        nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.MirrorImageReroll),
                    )
                } else {
                    resolveArmorClassHitSuccess()
                }
            }

            else -> resolveArmorClassHitSuccess()
        }

    private fun CombatActionArguments.resolveArmorClassHitSuccess(): RollOutcome {
        if (attackerOffensiveAbility == OffensiveAbility.SpellPotency && firstDie == secondDie) {
            return resolveSpellPotency()
        }

        return RollOutcome.Results(
            messages = messages,
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = attacker.userId,
            nextStep = CombatStep.HealthRoll,
        )
    }

    private fun CombatActionArguments.resolveSpellPotency(): RollOutcome {
        val splashDamage = diceTotal / 2

        // TODO: filter first to simplify logic in for loop

        updatedPlayerMap.values.forEach { player ->
            // TODO: introduce isKnockedOut for those waiting for the next Match?
            if (player.characterClass.hitPoints <= 0) return@forEach

            val damage =
                if (player.userId == defender.userId) {
                    diceTotal
                } else if (player.userId != attacker.userId) {
                    splashDamage
                } else {
                    0
                }

            if (damage > 0) {
                // TODO: The newHp should be whatever it is - less than 0 is acceptable (and
                // preferred is that's the true value)
                val newHp = (player.characterClass.hitPoints - damage)
                updatedPlayerMap[player.userId] =
                    player.copy(characterClass = player.characterClass.copy(hitPoints = newHp))
            }
        }

        // TODO: Make into function
        messages.add(
            element =
                OutgoingMessage.SpellPotency(
                    mageUserId = attacker.userId,
                    mainTargetUserId = defender.userId,
                    mainDamage = diceTotal,
                    splashDamage = splashDamage,
                )
        )

        return resolveTurnEnd()
    }

    private fun CombatActionArguments.resolveArmorClassMiss(): RollOutcome {
        if (defenderDefensiveAbility is DefensiveAbility.Dodge) {
            resolveDodge()
        }

        return resolveTurnEnd()
    }

    private fun CombatActionArguments.resolveDodge() {
        val dodge = defenderDefensiveAbility as DefensiveAbility.Dodge
        if (dodge.armorBonus < 2) {
            // TODO in UI layer: Add ability and AC together to get end result
            val updatedDodge = dodge.copy(armorBonus = dodge.armorBonus + 1)
            val updatedDefenderClass = defender.characterClass.copy(defensiveAbility = updatedDodge)
            val updatedDefender = defender.copy(characterClass = updatedDefenderClass)
            updatedPlayerMap[defender.userId] = updatedDefender

            messages.add(element = OutgoingMessage.Dodge(didArmorIncrease = true))
        } else {
            messages.add(element = OutgoingMessage.Dodge(didArmorIncrease = false))
        }
    }

    private fun CombatActionArguments.handleHealthRoll(): RollOutcome =
        if (diceTotal >= defender.characterClass.hitPoints) {
            addNormalRollMessage(type = NormalRollMessageType.Knockout)
            resolveKnockout()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.Damage)
            resolveDamage()
        }

    private fun CombatActionArguments.resolveKnockout(): RollOutcome {
        if (
            defenderDefensiveAbility is DefensiveAbility.HolyShield &&
                !defenderDefensiveAbility.usedThisMatch
        ) {
            return RollOutcome.Results(
                messages = messages,
                updatedPlayerMap = updatedPlayerMap,
                nextActivePlayerUserId = defender.userId,
                nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.HolyShieldSurvival),
            )
        }

        // TODO: hp doesn't need to be 0 - it's cooler if it's negative
        val updatedDefenderClass = defender.characterClass.copy(hitPoints = 0)
        updatedPlayerMap[defender.userId] = defender.copy(characterClass = updatedDefenderClass)

        if (attackerOffensiveAbility == OffensiveAbility.KillingMomentum) {
            messages.add(
                element =
                    OutgoingMessage.KillingMomentum(
                        rogueUserId = attacker.userId,
                        isTriggered = true,
                    )
            )
            return RollOutcome.Results(
                messages = messages,
                updatedPlayerMap = updatedPlayerMap,
                nextActivePlayerUserId = attacker.userId,
                nextStep = CombatStep.Resolution,
            )
        }

        return resolveTurnEnd()
    }

    private fun CombatActionArguments.resolveDamage(): RollOutcome {
        if (attackerOffensiveAbility == OffensiveAbility.SpellPotency && firstDie == secondDie) {
            return resolveSpellPotency()
        }

        if (attackerOffensiveAbility == OffensiveAbility.Encore) {
            return RollOutcome.Results(
                messages = messages,
                updatedPlayerMap = updatedPlayerMap,
                nextActivePlayerUserId = attacker.userId,
                nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.EncoreCheck),
            )
        }

        return resolveTurnEnd()
    }

    private fun CombatActionArguments.resolveTurnEnd(): RollOutcome {
        markAttackerAsActed()

        val nextPlayer = calculateNextPlayer()

        if (nextPlayer == null) {
            val newRoundNumber = roundNumber + 1
            resetAllPlayersActed()
            messages.add(element = OutgoingMessage.NewRound(roundNumber = newRoundNumber))

            // TODO: calculateNextPlayer() should be split into two functions then, to be explicit
            //  and not have a fallback "that should never happen"
            val fastestNextRound =
                calculateNextPlayer() ?: arguments.attacker // Fallback should not happen
            fastestNextRound.characterClass.addNewTurnMessage()

            return RollOutcome.Results(
                messages = messages,
                updatedPlayerMap = updatedPlayerMap,
                nextActivePlayerUserId = fastestNextRound.userId,
                nextStep = CombatStep.ArmorClassRoll,
            )
        }

        nextPlayer.characterClass.addNewTurnMessage()

        return RollOutcome.Results(
            messages = messages,
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = nextPlayer.userId,
            nextStep = CombatStep.ArmorClassRoll,
        )
    }

    private fun CombatActionArguments.markAttackerAsActed() {
        val attacker = updatedPlayerMap.getValue(key = attacker.userId)
        updatedPlayerMap[attacker.userId] =
            attacker.copy(characterClass = attacker.characterClass.copy(hasActedThisRound = true))
    }

    private fun calculateNextPlayer(): ArenaPlayer.Selected? {
        val livingPlayers = updatedPlayerMap.values.filter { it.characterClass.hitPoints > 0 }

        val candidates = livingPlayers.filter { !it.characterClass.hasActedThisRound }

        return candidates.maxByOrNull { it.characterClass.speed }
    }

    private fun resetAllPlayersActed() {
        updatedPlayerMap.entries.forEach { (userId, player) ->
            updatedPlayerMap[userId] =
                player.copy(characterClass = player.characterClass.copy(hasActedThisRound = false))
        }
    }

    private fun CombatActionArguments.addNormalRollMessage(type: NormalRollMessageType) {
        messages.add(
            element =
                OutgoingMessage.NormalRoll(
                    attackerUserId = attacker.userId,
                    defenderUserId = defender.userId,
                    firstDie = firstDie,
                    secondDie = secondDie,
                    type = type,
                )
        )
    }

    private fun CharacterClass.addNewTurnMessage() {
        val characterClassInfo = CharacterClassMessageInfo(className = className, type = type)
        messages.add(element = OutgoingMessage.NewTurn(characterClassInfo = characterClassInfo))
    }
}
