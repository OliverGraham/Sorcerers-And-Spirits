package com.linklabs.sorcerersandspirits.domain.arena.utilities

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatStep
import com.linklabs.sorcerersandspirits.domain.arena.model.MirrorImageStatus
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.RollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.usecase.CombatActionArguments
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.utilities.UserId

data class RollOutcome(
    val messages: List<OutgoingMessage>,
    val updatedPlayerMap: Map<UserId, ArenaPlayer.Selected>,
    val nextActivePlayerUserId: String,
    val nextStep: CombatStep,
)

internal class Attack(
    private val firstDie: Int,
    private val secondDie: Int,
    private val arguments: CombatActionArguments,
) {
    private val messages = mutableListOf<OutgoingMessage>()
    private val updatedPlayerMap = arguments.playerMap.toMutableMap()

    private val diceTotal = firstDie + secondDie

    internal data class Updates(val nextActivePlayerUserId: String, val nextStep: CombatStep)

    fun handleArmorClassRoll(): RollOutcome {
        val updates = arguments.handleArmorClassRoll()

        return RollOutcome(
            messages = messages,
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = updates.nextActivePlayerUserId,
            nextStep = updates.nextStep,
        )
    }

    fun handleHealthRoll(): RollOutcome {
        val updates = arguments.handleHealthRoll()

        return RollOutcome(
            messages = messages,
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = updates.nextActivePlayerUserId,
            nextStep = updates.nextStep,
        )
    }

    private fun CombatActionArguments.handleArmorClassRoll(): Updates =
        if (diceTotal >= defender.characterClass.armorClass) {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassHit)
            resolveArmorClassHit()
        } else {
            addNormalRollMessage(type = NormalRollMessageType.ArmorClassMiss)
            resolveArmorClassMiss()
        }

    private fun CombatActionArguments.resolveArmorClassHit(): Updates =
        when (defenderDefensiveAbility) {
            is DefensiveAbility.Teleport -> {
                Updates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.TeleportDefenderEscape),
                )
            }

            is DefensiveAbility.Riposte -> {
                Updates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.RiposteCounter),
                )
            }

            is DefensiveAbility.Persuade -> {
                Updates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.PersuadeStopOrRedirect),
                )
            }

            is DefensiveAbility.LuckyCoin -> {
                Updates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.LuckyCoinArmorBonus),
                )
            }

            is DefensiveAbility.MirrorImage -> {
                messages.add(
                    element =
                        OutgoingMessage.MirrorImage(
                            illusionistUserId = defender.userId,
                            status = MirrorImageStatus.Pondering,
                        )
                )
                Updates(
                    nextActivePlayerUserId = defender.userId,
                    nextStep =
                        CombatStep.AbilityRoll(
                            purpose =
                                RollPurpose.MirrorImageChoice(
                                    firstDie = firstDie,
                                    secondDie = secondDie,
                                    triggeredByArmorRoll = true,
                                )
                        ),
                )
            }

            else -> resolveArmorClassHitSuccess()
        }

    fun resolveAfterMirrorImageArmorRoll(extraMessages: List<OutgoingMessage>): RollOutcome {
        messages.addAll(extraMessages)

        markMirrorImageAsUsed()

        val updates =
            if (diceTotal >= arguments.defender.characterClass.armorClass) {
                // It still hits!
                arguments.addNormalRollMessage(type = NormalRollMessageType.ArmorClassHit)
                arguments.resolveArmorClassHitSuccess()
            } else {
                // It's now a miss!
                arguments.addNormalRollMessage(type = NormalRollMessageType.ArmorClassMiss)
                arguments.resolveArmorClassMiss()
            }

        return RollOutcome(
            messages = messages,
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = updates.nextActivePlayerUserId,
            nextStep = updates.nextStep,
        )
    }

    fun resolveAfterMirrorImageHealthRoll(extraMessages: List<OutgoingMessage>): RollOutcome {
        messages.addAll(extraMessages)

        markMirrorImageAsUsed()

        val updates =
            if (diceTotal >= arguments.defender.characterClass.hitPoints) {
                arguments.addNormalRollMessage(type = NormalRollMessageType.Knockout)
                arguments.resolveKnockout()
            } else {
                arguments.addNormalRollMessage(type = NormalRollMessageType.Damage)
                arguments.resolveDamage()
            }

        return RollOutcome(
            messages = messages,
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = updates.nextActivePlayerUserId,
            nextStep = updates.nextStep,
        )
    }

    private fun markMirrorImageAsUsed() {
        val defender = updatedPlayerMap.getValue(key = arguments.defender.userId)
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

    fun resumeHealthRollResolution(): RollOutcome {
        val updates =
            if (diceTotal >= arguments.defender.characterClass.hitPoints) {
                arguments.addNormalRollMessage(type = NormalRollMessageType.Knockout)
                arguments.resolveKnockout()
            } else {
                arguments.addNormalRollMessage(type = NormalRollMessageType.Damage)
                arguments.resolveDamage()
            }

        return RollOutcome(
            messages = messages,
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = updates.nextActivePlayerUserId,
            nextStep = updates.nextStep,
        )
    }

    internal fun CombatActionArguments.resolveArmorClassHitSuccess(): Updates {
        if (attackerOffensiveAbility == OffensiveAbility.SpellPotency && firstDie == secondDie) {
            return resolveSpellPotency()
        }

        return Updates(nextActivePlayerUserId = attacker.userId, nextStep = CombatStep.HealthRoll)
    }

    private fun CombatActionArguments.resolveSpellPotency(): Updates {
        val splashDamage = diceTotal / 2

        applyFullDamageToDefender()
        applySplashDamage(splashDamage = splashDamage)

        addSpellPotencyMessage(splashDamage = splashDamage)

        return resolveTurnEnd()
    }

    private fun CombatActionArguments.applyFullDamageToDefender() {
        val defenderPlayer = updatedPlayerMap.getValue(key = defender.userId)
        val newHp = defenderPlayer.characterClass.hitPoints - diceTotal
        updatedPlayerMap[defender.userId] =
            defenderPlayer.copy(
                characterClass = defenderPlayer.characterClass.copy(hitPoints = newHp)
            )
    }

    private fun CombatActionArguments.applySplashDamage(splashDamage: Int) {
        updatedPlayerMap.values
            .filter { it.userId != attacker.userId && it.userId != defender.userId }
            .filter { it.characterClass.hitPoints > 0 }
            .forEach { player ->
                val newHp = player.characterClass.hitPoints - splashDamage
                updatedPlayerMap[player.userId] =
                    player.copy(characterClass = player.characterClass.copy(hitPoints = newHp))
            }
    }

    private fun CombatActionArguments.addSpellPotencyMessage(splashDamage: Int) {
        messages.add(
            element =
                OutgoingMessage.SpellPotency(
                    mageUserId = attacker.userId,
                    mainTargetUserId = defender.userId,
                    mainDamage = diceTotal,
                    splashDamage = splashDamage,
                )
        )
    }

    private fun CombatActionArguments.resolveArmorClassMiss(): Updates {
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

    // TODO: Update this, come on

    private fun CombatActionArguments.handleHealthRoll(): Updates {
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

    internal fun CombatActionArguments.resolveKnockout(): Updates {
        if (
            defenderDefensiveAbility is DefensiveAbility.HolyShield &&
                !defenderDefensiveAbility.usedThisMatch
        ) {
            return Updates(
                nextActivePlayerUserId = defender.userId,
                nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.HolyShieldSurvival),
            )
        }

        val newHp = defender.characterClass.hitPoints - diceTotal
        val updatedDefenderClass = defender.characterClass.copy(hitPoints = newHp)
        updatedPlayerMap[defender.userId] = defender.copy(characterClass = updatedDefenderClass)

        if (attackerOffensiveAbility == OffensiveAbility.KillingMomentum) {
            messages.add(
                element =
                    OutgoingMessage.KillingMomentum(
                        rogueUserId = attacker.userId,
                        isTriggered = true,
                    )
            )
            return Updates(
                nextActivePlayerUserId = attacker.userId,
                nextStep = CombatStep.Resolution,
            )
        }

        return resolveTurnEnd()
    }

    internal fun CombatActionArguments.resolveDamage(): Updates {
        if (attackerOffensiveAbility == OffensiveAbility.SpellPotency && firstDie == secondDie) {
            return resolveSpellPotency()
        }

        if (attackerOffensiveAbility == OffensiveAbility.Encore) {
            return Updates(
                nextActivePlayerUserId = attacker.userId,
                nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.EncoreCheck),
            )
        }

        return resolveTurnEnd()
    }

    private fun CombatActionArguments.resolveTurnEnd(): Updates {
        markAttackerAsActed()

        val nextPlayer = calculateNextPlayerInRound()

        if (nextPlayer == null) {
            val newRoundNumber = roundNumber + 1
            resetAllPlayersActed()
            messages.add(element = OutgoingMessage.NewRound(roundNumber = newRoundNumber))

            val fastestNextRound = calculateFastestLivingPlayer()
            fastestNextRound.characterClass.addNewTurnMessage()

            return Updates(
                nextActivePlayerUserId = fastestNextRound.userId,
                nextStep = CombatStep.ArmorClassRoll,
            )
        }

        nextPlayer.characterClass.addNewTurnMessage()

        return Updates(
            nextActivePlayerUserId = nextPlayer.userId,
            nextStep = CombatStep.ArmorClassRoll,
        )
    }

    private fun markAttackerAsActed() {
        val attacker = updatedPlayerMap.getValue(key = arguments.attacker.userId)
        updatedPlayerMap[attacker.userId] =
            attacker.copy(characterClass = attacker.characterClass.copy(hasActedThisRound = true))
    }

    private fun calculateNextPlayerInRound(): ArenaPlayer.Selected? {
        val livingPlayers = updatedPlayerMap.values.filter { it.characterClass.hitPoints > 0 }

        val candidates = livingPlayers.filter { !it.characterClass.hasActedThisRound }

        return candidates.maxByOrNull { it.characterClass.speed }
    }

    private fun calculateFastestLivingPlayer(): ArenaPlayer.Selected {
        val livingPlayers = updatedPlayerMap.values.filter { it.characterClass.hitPoints > 0 }

        return livingPlayers.maxBy { it.characterClass.speed }
    }

    private fun resetAllPlayersActed() {
        updatedPlayerMap.entries.forEach { (userId, player) ->
            val defensiveAbility = player.characterClass.defensiveAbility
            val updatedDefensiveAbility =
                if (defensiveAbility is DefensiveAbility.MirrorImage) {
                    defensiveAbility.copy(usedThisAttack = false)
                } else {
                    defensiveAbility
                }

            updatedPlayerMap[userId] =
                player.copy(
                    characterClass =
                        player.characterClass.copy(
                            hasActedThisRound = false,
                            defensiveAbility = updatedDefensiveAbility,
                        )
                )
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
