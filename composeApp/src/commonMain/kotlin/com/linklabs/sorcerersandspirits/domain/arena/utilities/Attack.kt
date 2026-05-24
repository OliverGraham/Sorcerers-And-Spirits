package com.linklabs.sorcerersandspirits.domain.arena.utilities

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

// TODO: Probably move this back to the use case and pass in arguments, instead of class-wide state

// TODO: What about the possible +1 from doubles from the previous roll?

// TODO Saturday: Need to figure out what all needs to be passed along to data layer
//  What was rolled, who is involved, abilities used, the outcome.
//  Lots of new message types I think?

// TODO: What game-state data needs to be here?
//  All players
//  Player who is going next
//      Abilities and such should be affected in the player map?

// TODO Sunday: RollOutcome sealed class
//  Normal (with 4 types)
//  Otherwise, type for each ability
//

sealed interface RollOutcome {

    data class Normal(
        val attackerUserId: String,
        val defenderUserId: String,
        val firstDie: Int,
        val secondDie: Int,
        val nextActivePlayerUserId: String,
        val playerMap: Map<UserId, ArenaPlayer.Selected>,
        val type: NormalRollType,
    ) : RollOutcome
}

enum class NormalRollType {
    // "A targeted D, rolled R and hit/missed"
    ArmorClassHit,
    ArmorClassMiss,

    // "A damaged D for R (if KO) and knocked out D!"
    Damage,
    Knockout,
}

internal class Attack(
    private val firstDie: Int,
    private val secondDie: Int,
    private val arguments: CombatActionArguments,
) {
    private val messages = mutableListOf<OutgoingMessage>()
    private val updatedPlayerMap = arguments.playerMap.toMutableMap()

    private val diceTotal = firstDie + secondDie

    fun handleArmorClassRoll() {
        arguments.handleArmorClassRoll()
    }

    fun handleHealthRoll() {
        arguments.handleHealthRoll()
    }

    private fun CombatActionArguments.handleArmorClassRoll(): RollOutcome =
        if (diceTotal >= defender.characterClass.armorClass) {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassHit)
            resolveArmorClassHit()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassMiss)
            resolveArmorClassMissAbilities()
            resolveArmorClassMiss()
        }

    private fun CombatActionArguments.resolveArmorClassHit(): RollOutcome {
        when (defenderDefensiveAbility) {
            is DefensiveAbility.Teleport -> {
                // TODO: handle Teleport - Teleport message
                //  Is there some way to
                //  who attacked, who defended, all players, who's up to roll now (mage), and then
                //    attacker -
            }

            // TODO: Gambler ability (can't remember)
            // TODO: Duelist Riposte
            // TODO: Bard Persuade
            // TODO: Illusionist reroll option (is this truly everywhere?)
            else -> {
                // TODO: Resolve normal AC hit - keep same target
            }
        }
    }

    private fun CombatActionArguments.resolveArmorClassMissAbilities() {
        // No other class does anything after a miss to their AC right?
        if (defenderDefensiveAbility is DefensiveAbility.Dodge) {
            val armorBonus = defenderDefensiveAbility.armorBonus
            if (armorBonus < 2) {
                val updatedDodge = defenderDefensiveAbility.copy(armorBonus = armorBonus + 1)
                val updatedDefenderClass =
                    defender.characterClass.copy(defensiveAbility = updatedDodge)
                val updatedDefender = defender.copy(characterClass = updatedDefenderClass)
                updatedPlayerMap[defender.userId] = updatedDefender

                addDodgeMessage(didArmorIncrease = true)
            } else {
                addDodgeMessage(didArmorIncrease = false)
            }
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

    private fun addDodgeMessage(didArmorIncrease: Boolean) {
        messages.add(element = OutgoingMessage.Dodge(didArmorIncrease = didArmorIncrease))
    }

    private fun CombatActionArguments.resolveArmorClassMiss(): RollOutcome {
        when (attackerOffensiveAbility) {
            // TODO: Gambler could have another turn
            // TODO: Necro could have another turn
            else -> {
                // TODO: normal turn-change update
                updatedPlayerMap[attacker.userId] =
                    attacker.updateHasActedThisRound(hasActedThisRound = true)

                val listOfWhoHasNotActedThisTurn =
                    updatedPlayerMap.values.filterNot { it.characterClass.hasActedThisRound }

                val nextUpArenaPlayer =
                    if (listOfWhoHasNotActedThisTurn.isEmpty()) {
                        // TODO: This is a new round
                        addNewRoundMessage()

                        updatedPlayerMap.entries.forEach { entry ->
                            entry.setValue(
                                newValue =
                                    entry.value.updateHasActedThisRound(hasActedThisRound = false)
                            )
                        }

                        updatedPlayerMap.values.findFastestCharacter()
                    } else {
                        // TODO: This is the same round, and this player is upNext
                        listOfWhoHasNotActedThisTurn.findFastestCharacter()
                    }

                nextUpArenaPlayer.characterClass.addNewTurnMessage()

                return RollOutcome(
                    nextActivePlayerUserId = nextUpArenaPlayer.userId,
                    playerMap = updatedPlayerMap,
                )
            }
        }
    }

    private fun CombatActionArguments.addNewRoundMessage() {
        messages.add(element = OutgoingMessage.NewRound(roundNumber = roundNumber + 1))
    }

    private fun CharacterClass.addNewTurnMessage() {
        val characterClassInfo = CharacterClassMessageInfo(className = className, type = type)

        messages.add(element = OutgoingMessage.NewTurn(characterClassInfo = characterClassInfo))
    }

    private fun CombatActionArguments.handleHealthRoll() {
        if (firstDie + secondDie >= defender.characterClass.hitPoints) {
            resolveKnockout()
        } else {
            resolveDamage()
        }

        // TODO: Bard's Encore
    }

    private fun CombatActionArguments.resolveKnockout() {
        if (defenderDefensiveAbility is DefensiveAbility.HolyShield) {
            if (!defenderDefensiveAbility.usedThisMatch) {
                // TODO: Handle Paladin bubble - if survive, then come back here and handle
                // regularly
            }
        }

        if (attackerOffensiveAbility == OffensiveAbility.KillingMomentum) {
            // TODO: Handle KillingMomentum - carry +1 from doubles if applicable
        }

        // TODO: otherwise, normal character knock out
    }

    private fun CombatActionArguments.resolveDamage() {
        when (attackerOffensiveAbility) {
            // TODO: Gambler could have another turn
            // TODO: Necro could have another turn
            else -> {}
        }
    }

    private fun Collection<ArenaPlayer.Selected>.findFastestCharacter() = maxBy { player ->
        player.characterClass.speed
    }

    private fun ArenaPlayer.Selected.updateHasActedThisRound(hasActedThisRound: Boolean) =
        copy(characterClass = characterClass.copy(hasActedThisRound = hasActedThisRound))
}
