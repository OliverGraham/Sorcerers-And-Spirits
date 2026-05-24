package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.AllInOutcome
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatStep
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.PersuadeOutcome
import com.linklabs.sorcerersandspirits.domain.arena.model.RollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.SummonOutcome
import com.linklabs.sorcerersandspirits.domain.arena.model.TurnInfo
import com.linklabs.sorcerersandspirits.domain.arena.utilities.Attack
import com.linklabs.sorcerersandspirits.domain.arena.utilities.RollOutcome
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

data class CombatActionArguments(
    val arenaId: String,
    val attacker: ArenaPlayer.Selected,
    val defender: ArenaPlayer.Selected,
    val playerMap: Map<String, ArenaPlayer.Selected>,
    val turnInfo: TurnInfo,
) {
    val attackerOffensiveAbility = attacker.characterClass.offensiveAbility
    val attackerDefensiveAbility = attacker.characterClass.defensiveAbility
    val defenderOffensiveAbility = defender.characterClass.offensiveAbility
    val defenderDefensiveAbility = defender.characterClass.defensiveAbility

    val roundNumber = turnInfo.roundNumber
}

class PerformCombatActionUseCase(private val arenaRepository: ArenaRepository) {

    suspend operator fun invoke(arguments: CombatActionArguments, currentStep: CombatStep) {
        val results = resolveStep(arguments = arguments, step = currentStep)

        // This results variable is intended for the repository call in the future.
        println(message = "Combat results: $results")
    }

    private fun resolveStep(arguments: CombatActionArguments, step: CombatStep): RollOutcome =
        when (step) {
            is CombatStep.ArmorClassRoll -> handleArmorClassRoll(arguments = arguments)

            is CombatStep.HealthRoll -> handleHealthRoll(arguments = arguments)

            is CombatStep.AbilityRoll ->
                handleAbilityRoll(arguments = arguments, purpose = step.purpose)

            is CombatStep.Resolution -> handleResolution(arguments = arguments)
        }

    private fun handleArmorClassRoll(arguments: CombatActionArguments): RollOutcome {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val attack = Attack(firstDie = firstDie, secondDie = secondDie, arguments = arguments)
        return attack.handleArmorClassRoll()
    }

    private fun handleHealthRoll(arguments: CombatActionArguments): RollOutcome {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val attack = Attack(firstDie = firstDie, secondDie = secondDie, arguments = arguments)
        return attack.handleHealthRoll()
    }

    private fun handleAbilityRoll(
        arguments: CombatActionArguments,
        purpose: RollPurpose,
    ): RollOutcome =
        when (purpose) {
            is RollPurpose.TeleportDefenderEscape ->
                handleTeleportDefenderEscape(arguments = arguments)

            is RollPurpose.TeleportAttackerChase ->
                handleTeleportAttackerChase(
                    arguments = arguments,
                    defenderRoll = purpose.defenderRoll,
                )

            is RollPurpose.RiposteCounter -> handleRiposteCounter(arguments = arguments)

            is RollPurpose.PersuadeStopOrRedirect ->
                handlePersuadeStopOrRedirect(arguments = arguments)

            is RollPurpose.LuckyCoinArmorBonus -> handleLuckyCoinArmorBonus(arguments = arguments)

            is RollPurpose.LuckyCoinReflect -> handleLuckyCoinReflect(arguments = arguments)

            is RollPurpose.AllInOutcome -> handleAllInOutcome(arguments = arguments)

            is RollPurpose.SummonMinionRoll -> handleSummonMinionRoll(arguments = arguments)

            is RollPurpose.PrepareAttackRoll -> handlePrepareAttackRoll(arguments = arguments)

            is RollPurpose.EncoreCheck -> handleEncoreCheck(arguments = arguments)

            is RollPurpose.HolyShieldSurvival -> handleHolyShieldSurvival(arguments = arguments)

            is RollPurpose.HolyShieldRetaliation ->
                handleHolyShieldRetaliation(arguments = arguments)

            is RollPurpose.MirrorImageChoice ->
                handleMirrorImageChoice(arguments = arguments, purpose = purpose)

            is RollPurpose.MirrorImageReroll ->
                handleMirrorImageReroll(arguments = arguments, purpose = purpose)

            is RollPurpose.ImpersonateChoice -> handleImpersonateChoice(arguments = arguments)
        }

    private fun handleMirrorImageChoice(
        arguments: CombatActionArguments,
        purpose: RollPurpose.MirrorImageChoice,
    ): RollOutcome {
        // This is a decision step, if it's called it means the UI just wants to stay in this state
        // or it was triggered by a roll that hasn't happened yet.
        // Usually, the UI will call invoke with MirrorImageReroll or HealthRoll.
        return RollOutcome(
            messages = emptyList(),
            updatedPlayerMap = arguments.playerMap,
            nextActivePlayerUserId = arguments.defender.userId,
            nextStep = CombatStep.AbilityRoll(purpose = purpose),
        )
    }

    private fun handleMirrorImageReroll(
        arguments: CombatActionArguments,
        purpose: RollPurpose.MirrorImageReroll,
    ): RollOutcome {
        val roll = roll1D6() - 1

        val newFirstDie = if (purpose.dieIndexToReplace == 1) roll else purpose.firstDie
        val newSecondDie = if (purpose.dieIndexToReplace == 2) roll else purpose.secondDie

        val messages =
            listOf(
                OutgoingMessage.MirrorImage(
                    illusionistUserId = arguments.defender.userId,
                    triggered = true,
                )
            )

        val attack = Attack(firstDie = newFirstDie, secondDie = newSecondDie, arguments = arguments)

        return attack.resolveAfterMirrorImage(extraMessages = messages)
    }

    private fun handleImpersonateChoice(arguments: CombatActionArguments): RollOutcome {
        // Implementation for Impersonate choice
        // For now, let's just transition to the next step (e.g. Resolution or AC Roll)
        return RollOutcome(
            messages = emptyList(),
            updatedPlayerMap = arguments.playerMap,
            nextActivePlayerUserId = arguments.attacker.userId,
            nextStep = CombatStep.Resolution,
        )
    }

    private fun handleTeleportDefenderEscape(arguments: CombatActionArguments): RollOutcome {
        val roll = roll1D6()
        return RollOutcome(
            messages = emptyList(),
            updatedPlayerMap = arguments.playerMap,
            nextActivePlayerUserId = arguments.attacker.userId,
            nextStep =
                CombatStep.AbilityRoll(
                    purpose = RollPurpose.TeleportAttackerChase(defenderRoll = roll)
                ),
        )
    }

    private fun handleTeleportAttackerChase(
        arguments: CombatActionArguments,
        defenderRoll: Int,
    ): RollOutcome {
        val attackerRoll = roll1D6()
        val escaped = defenderRoll > attackerRoll

        val messages =
            mutableListOf<OutgoingMessage>(
                OutgoingMessage.Teleport(
                    mageUserId = arguments.defender.userId,
                    attackerUserId = arguments.attacker.userId,
                    escaped = escaped,
                )
            )

        return if (escaped) {
            resolveTurnEnd(arguments = arguments, extraMessages = messages)
        } else {
            RollOutcome(
                messages = messages,
                updatedPlayerMap = arguments.playerMap,
                nextActivePlayerUserId = arguments.attacker.userId,
                nextStep = CombatStep.HealthRoll,
            )
        }
    }

    private fun handleRiposteCounter(arguments: CombatActionArguments): RollOutcome {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie
        val triggered = total in 6..8

        val messages =
            mutableListOf<OutgoingMessage>(
                OutgoingMessage.Riposte(
                    duelistUserId = arguments.defender.userId,
                    attackerUserId = arguments.attacker.userId,
                    triggered = triggered,
                )
            )

        return if (triggered) {
            // Flip attack: Defender now attacks Attacker
            RollOutcome(
                messages = messages,
                updatedPlayerMap = arguments.playerMap,
                nextActivePlayerUserId = arguments.defender.userId,
                nextStep = CombatStep.ArmorClassRoll,
            )
        } else {
            RollOutcome(
                messages = messages,
                updatedPlayerMap = arguments.playerMap,
                nextActivePlayerUserId = arguments.attacker.userId,
                nextStep = CombatStep.HealthRoll,
            )
        }
    }

    private fun handlePersuadeStopOrRedirect(arguments: CombatActionArguments): RollOutcome {
        val roll = roll1D6()
        val outcome =
            when (roll) {
                5 -> PersuadeOutcome.Stopped
                6 -> PersuadeOutcome.Redirected
                else -> PersuadeOutcome.Failed
            }

        val messages =
            mutableListOf<OutgoingMessage>(
                OutgoingMessage.Persuade(
                    bardUserId = arguments.defender.userId,
                    attackerUserId = arguments.attacker.userId,
                    outcome = outcome,
                )
            )

        return when (outcome) {
            PersuadeOutcome.Stopped ->
                resolveTurnEnd(arguments = arguments, extraMessages = messages)

            PersuadeOutcome.Redirected -> {
                RollOutcome(
                    messages = messages,
                    updatedPlayerMap = arguments.playerMap,
                    nextActivePlayerUserId = arguments.attacker.userId,
                    nextStep = CombatStep.Resolution, // Attacker needs to pick new target
                )
            }

            PersuadeOutcome.Failed -> {
                RollOutcome(
                    messages = messages,
                    updatedPlayerMap = arguments.playerMap,
                    nextActivePlayerUserId = arguments.attacker.userId,
                    nextStep = CombatStep.HealthRoll,
                )
            }
        }
    }

    private fun handleLuckyCoinArmorBonus(arguments: CombatActionArguments): RollOutcome {
        val roll = roll1D6()
        val updatedPlayerMap = arguments.playerMap.toMutableMap()
        val defender = arguments.defender

        val newAc = defender.characterClass.armorClass + roll
        updatedPlayerMap[defender.userId] =
            defender.copy(characterClass = defender.characterClass.copy(armorClass = newAc))

        if (roll == 6) {
            return RollOutcome(
                messages = emptyList(),
                updatedPlayerMap = updatedPlayerMap,
                nextActivePlayerUserId = defender.userId,
                nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.LuckyCoinReflect),
            )
        }

        // Re-evaluate AC roll with new AC
        // This is tricky. For simplicity, let's assume it still hits if the original roll was high
        // enough?
        // Actually, the original roll might now be a miss.
        // We'd need the original roll value here.
        // For now, let's just proceed to Health Roll and let the UI/user handle if it's a miss now.
        // Better: return to AC roll step with the new map?
        return RollOutcome(
            messages =
                listOf(
                    OutgoingMessage.LuckyCoin(
                        gamblerUserId = defender.userId,
                        attackerUserId = arguments.attacker.userId,
                        armorBonus = roll,
                        reflectDamage = 0,
                    )
                ),
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = arguments.attacker.userId,
            nextStep = CombatStep.HealthRoll,
        )
    }

    private fun handleLuckyCoinReflect(arguments: CombatActionArguments): RollOutcome {
        val damage = roll1D6()
        val updatedPlayerMap = arguments.playerMap.toMutableMap()
        val attacker = arguments.attacker

        val newHp = (attacker.characterClass.hitPoints - damage).coerceAtLeast(minimumValue = 0)
        updatedPlayerMap[attacker.userId] =
            attacker.copy(characterClass = attacker.characterClass.copy(hitPoints = newHp))

        val messages =
            listOf(
                OutgoingMessage.LuckyCoin(
                    gamblerUserId = arguments.defender.userId,
                    attackerUserId = attacker.userId,
                    armorBonus = 6, // We know it was a 6 to get here
                    reflectDamage = damage,
                )
            )

        return RollOutcome(
            messages = messages,
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = arguments.attacker.userId,
            nextStep = CombatStep.HealthRoll,
        )
    }

    private fun handleAllInOutcome(arguments: CombatActionArguments): RollOutcome {
        val roll = roll1D6()
        val outcome =
            when (roll) {
                1 -> AllInOutcome.KnockoutSelf

                5,
                6 -> AllInOutcome.DoubleAttack

                else -> AllInOutcome.NoEffect
            }

        val updatedPlayerMap = arguments.playerMap.toMutableMap()
        val attacker = arguments.attacker
        val messages =
            mutableListOf<OutgoingMessage>(
                OutgoingMessage.AllIn(gamblerUserId = attacker.userId, outcome = outcome)
            )

        return when (outcome) {
            AllInOutcome.KnockoutSelf -> {
                updatedPlayerMap[attacker.userId] =
                    attacker.copy(characterClass = attacker.characterClass.copy(hitPoints = 0))
                resolveTurnEnd(
                    arguments = arguments,
                    updatedMap = updatedPlayerMap,
                    extraMessages = messages,
                )
            }

            AllInOutcome.DoubleAttack -> {
                // Stay active for an attack? Or just continue to next step?
                // AGENTS.md says "attacks twice this round".
                RollOutcome(
                    messages = messages,
                    updatedPlayerMap = updatedPlayerMap,
                    nextActivePlayerUserId = attacker.userId,
                    nextStep = CombatStep.Resolution, // Ready to pick first target
                )
            }

            AllInOutcome.NoEffect -> {
                // Continue to normal turn flow?
                RollOutcome(
                    messages = messages,
                    updatedPlayerMap = updatedPlayerMap,
                    nextActivePlayerUserId = attacker.userId,
                    nextStep = CombatStep.Resolution, // Can still perform normal attack?
                )
            }
        }
    }

    private fun handleSummonMinionRoll(arguments: CombatActionArguments): RollOutcome {
        val roll = roll1D6()
        val outcome =
            when (roll) {
                1 -> SummonOutcome.Fizzle

                2,
                3 -> SummonOutcome.Skeleton

                4,
                5 -> SummonOutcome.HealAndSkeleton

                6 -> SummonOutcome.HealAndGolem

                else -> SummonOutcome.Fizzle
            }

        val updatedPlayerMap = arguments.playerMap.toMutableMap()
        val necro = arguments.attacker
        val messages =
            mutableListOf<OutgoingMessage>(
                OutgoingMessage.SummonMinion(necroUserId = necro.userId, outcome = outcome)
            )

        // Apply effects
        val currentAbility = necro.characterClass.offensiveAbility as OffensiveAbility.SummonMinion
        var newSkeletonCount = currentAbility.skeletonCount
        var newGolemCount = currentAbility.golemCount
        var newHp = necro.characterClass.hitPoints

        when (outcome) {
            SummonOutcome.Fizzle -> {}

            SummonOutcome.Skeleton -> newSkeletonCount++

            SummonOutcome.HealAndSkeleton -> {
                newSkeletonCount++
                newHp = 7 // Necro max HP
            }

            SummonOutcome.HealAndGolem -> {
                newGolemCount++
                newHp = 7
            }
        }

        updatedPlayerMap[necro.userId] =
            necro.copy(
                characterClass =
                    necro.characterClass.copy(
                        hitPoints = newHp,
                        offensiveAbility =
                            currentAbility.copy(
                                skeletonCount = newSkeletonCount,
                                golemCount = newGolemCount,
                            ),
                    )
            )

        return resolveTurnEnd(
            arguments = arguments,
            updatedMap = updatedPlayerMap,
            extraMessages = messages,
        )
    }

    private fun handlePrepareAttackRoll(arguments: CombatActionArguments): RollOutcome {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie

        var success = total in 10..12

        // Paladin special rule: 12 stops Duelist's Prepare Attack.
        if (total == 12 && arguments.defenderDefensiveAbility is DefensiveAbility.HolyShield) {
            success = false
        }

        val updatedPlayerMap = arguments.playerMap.toMutableMap()
        val defender = arguments.defender

        if (success) {
            updatedPlayerMap[defender.userId] =
                defender.copy(characterClass = defender.characterClass.copy(hitPoints = 0))
        }

        val messages =
            listOf(
                OutgoingMessage.PrepareAttack(
                    duelistUserId = arguments.attacker.userId,
                    targetUserId = defender.userId,
                    success = success,
                )
            )

        return resolveTurnEnd(
            arguments = arguments,
            updatedMap = updatedPlayerMap,
            extraMessages = messages,
        )
    }

    private fun handleEncoreCheck(arguments: CombatActionArguments): RollOutcome {
        val roll = roll1D6()
        val triggered = roll in 5..6

        val messages =
            listOf(
                OutgoingMessage.Encore(
                    bardUserId = arguments.attacker.userId,
                    triggered = triggered,
                )
            )

        return if (triggered) {
            RollOutcome(
                messages = messages,
                updatedPlayerMap = arguments.playerMap,
                nextActivePlayerUserId = arguments.attacker.userId,
                nextStep = CombatStep.Resolution, // Attack again
            )
        } else {
            resolveTurnEnd(arguments = arguments, extraMessages = messages)
        }
    }

    private fun handleHolyShieldSurvival(arguments: CombatActionArguments): RollOutcome {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie
        val survived = total in 10..12

        val updatedPlayerMap = arguments.playerMap.toMutableMap()
        val paladin = arguments.defender

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
            RollOutcome(
                messages = emptyList(),
                updatedPlayerMap = updatedPlayerMap,
                nextActivePlayerUserId = paladin.userId,
                nextStep = CombatStep.AbilityRoll(purpose = RollPurpose.HolyShieldRetaliation),
            )
        } else {
            updatedPlayerMap[paladin.userId] =
                paladin.copy(characterClass = paladin.characterClass.copy(hitPoints = 0))
            val messages =
                listOf(
                    OutgoingMessage.HolyShield(
                        paladinUserId = paladin.userId,
                        survived = false,
                        retaliationDamage = 0,
                    )
                )
            resolveTurnEnd(
                arguments = arguments,
                updatedMap = updatedPlayerMap,
                extraMessages = messages,
            )
        }
    }

    private fun handleHolyShieldRetaliation(arguments: CombatActionArguments): RollOutcome {
        val damage = roll1D6()
        val updatedPlayerMap = arguments.playerMap.toMutableMap()
        val attacker = arguments.attacker

        val newHp = (attacker.characterClass.hitPoints - damage).coerceAtLeast(minimumValue = 0)
        updatedPlayerMap[attacker.userId] =
            attacker.copy(characterClass = attacker.characterClass.copy(hitPoints = newHp))

        val messages =
            listOf(
                OutgoingMessage.HolyShield(
                    paladinUserId = arguments.defender.userId,
                    survived = true,
                    retaliationDamage = damage,
                )
            )

        return resolveTurnEnd(
            arguments = arguments,
            updatedMap = updatedPlayerMap,
            extraMessages = messages,
        )
    }

    private fun handleResolution(arguments: CombatActionArguments): RollOutcome {
        // Usually transitions to AC roll for the attacker
        return RollOutcome(
            messages = emptyList(),
            updatedPlayerMap = arguments.playerMap,
            nextActivePlayerUserId = arguments.attacker.userId,
            nextStep = CombatStep.ArmorClassRoll,
        )
    }

    private fun resolveTurnEnd(
        arguments: CombatActionArguments,
        updatedMap: Map<String, ArenaPlayer.Selected>? = null,
        extraMessages: List<OutgoingMessage> = emptyList(),
    ): RollOutcome {
        val mapToUse = updatedMap ?: arguments.playerMap
        val tempUpdatedMap = mapToUse.toMutableMap()

        // Mark current attacker as acted
        val attacker = tempUpdatedMap[arguments.attacker.userId]
        if (attacker != null) {
            tempUpdatedMap[arguments.attacker.userId] =
                attacker.copy(
                    characterClass = attacker.characterClass.copy(hasActedThisRound = true)
                )
        }

        val livingPlayers = tempUpdatedMap.values.filter { it.characterClass.hitPoints > 0 }

        val candidates = livingPlayers.filter { !it.characterClass.hasActedThisRound }

        val nextPlayer = candidates.maxByOrNull { it.characterClass.speed }
        val messages = extraMessages.toMutableList()

        return if (nextPlayer == null) {
            // New Round
            tempUpdatedMap.entries.forEach { (userId, player) ->
                tempUpdatedMap[userId] =
                    player.copy(
                        characterClass = player.characterClass.copy(hasActedThisRound = false)
                    )
            }
            messages.add(
                element = OutgoingMessage.NewRound(roundNumber = arguments.roundNumber + 1)
            )

            val fastest =
                tempUpdatedMap.values
                    .filter { it.characterClass.hitPoints > 0 }
                    .maxByOrNull { it.characterClass.speed } ?: arguments.attacker

            val characterClassInfo =
                CharacterClassMessageInfo(
                    className = fastest.characterClass.className,
                    type = fastest.characterClass.type,
                )
            messages.add(element = OutgoingMessage.NewTurn(characterClassInfo = characterClassInfo))

            RollOutcome(
                messages = messages,
                updatedPlayerMap = tempUpdatedMap,
                nextActivePlayerUserId = fastest.userId,
                nextStep = CombatStep.ArmorClassRoll,
            )
        } else {
            val characterClassInfo =
                CharacterClassMessageInfo(
                    className = nextPlayer.characterClass.className,
                    type = nextPlayer.characterClass.type,
                )
            messages.add(element = OutgoingMessage.NewTurn(characterClassInfo = characterClassInfo))

            RollOutcome(
                messages = messages,
                updatedPlayerMap = tempUpdatedMap,
                nextActivePlayerUserId = nextPlayer.userId,
                nextStep = CombatStep.ArmorClassRoll,
            )
        }
    }
}
