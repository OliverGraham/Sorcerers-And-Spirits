package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.AllInOutcome
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatStep
import com.linklabs.sorcerersandspirits.domain.arena.model.MirrorImageStatus
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
        val results = arguments.resolveStep(step = currentStep)

        // This results variable is intended for the repository call in the future.
        println(message = "Combat results: $results")
    }

    private fun CombatActionArguments.resolveStep(step: CombatStep): RollOutcome =
        when (step) {
            is CombatStep.ArmorClassRoll -> handleArmorClassRoll()
            is CombatStep.HealthRoll -> handleHealthRoll()
            is CombatStep.AbilityRoll -> handleAbilityRoll(purpose = step.purpose)
            is CombatStep.Resolution -> handleResolution()
        }

    private fun CombatActionArguments.handleArmorClassRoll(): RollOutcome {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val attack = Attack(firstDie = firstDie, secondDie = secondDie, arguments = this)
        return attack.handleArmorClassRoll()
    }

    private fun CombatActionArguments.handleHealthRoll(): RollOutcome {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val attack = Attack(firstDie = firstDie, secondDie = secondDie, arguments = this)
        return attack.handleHealthRoll()
    }

    private fun CombatActionArguments.handleAbilityRoll(purpose: RollPurpose): RollOutcome =
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

    private fun CombatActionArguments.handleMirrorImageChoice(
        purpose: RollPurpose.MirrorImageChoice
    ): RollOutcome {
        // This is a decision step where the Illusionist chooses whether to reroll a die or pass.
        // Returning the same state allows the UI to display the choice and wait for user input.
        // To exit this state, the UI should call invoke with either MirrorImagePass or
        // MirrorImageReroll.
        return RollOutcome(
            messages = emptyList(),
            updatedPlayerMap = playerMap,
            nextActivePlayerUserId = defender.userId,
            nextStep = CombatStep.AbilityRoll(purpose = purpose),
        )
    }

    private fun CombatActionArguments.handleMirrorImagePass(
        purpose: RollPurpose.MirrorImagePass
    ): RollOutcome {
        // If the Illusionist chooses not to reroll, the hit/damage stands and combat resumes.
        val messages =
            listOf(
                OutgoingMessage.MirrorImage(
                    illusionistUserId = defender.userId,
                    status = MirrorImageStatus.Passed,
                )
            )

        return if (purpose.triggeredByArmorRoll) {
            // Hit stands, move to damage roll
            RollOutcome(
                messages = messages,
                updatedPlayerMap = playerMap,
                nextActivePlayerUserId = attacker.userId,
                nextStep = CombatStep.HealthRoll,
            )
        } else {
            // Damage roll stands, finalize resolution using original dice
            val attack =
                Attack(firstDie = purpose.firstDie, secondDie = purpose.secondDie, arguments = this)
            val finalOutcome = attack.resumeHealthRollResolution()
            finalOutcome.copy(messages = messages + finalOutcome.messages)
        }
    }

    private fun CombatActionArguments.handleMirrorImageReroll(
        purpose: RollPurpose.MirrorImageReroll
    ): RollOutcome {
        val roll = roll1D6() - 1

        val newFirstDie = if (purpose.rerollFirstDie) roll else purpose.firstDie
        val newSecondDie = if (!purpose.rerollFirstDie) roll else purpose.secondDie

        val messages =
            listOf(
                OutgoingMessage.MirrorImage(
                    illusionistUserId = defender.userId,
                    status = MirrorImageStatus.Rerolled,
                )
            )

        val attack = Attack(firstDie = newFirstDie, secondDie = newSecondDie, arguments = this)

        return if (purpose.triggeredByArmorRoll) {
            attack.resolveAfterMirrorImageArmorRoll(extraMessages = messages)
        } else {
            attack.resolveAfterMirrorImageHealthRoll(extraMessages = messages)
        }
    }

    private fun CombatActionArguments.handleImpersonateChoice(): RollOutcome {
        // Implementation for Impersonate choice
        // For now, let's just transition to the next step (e.g. Resolution or AC Roll)
        return RollOutcome(
            messages = emptyList(),
            updatedPlayerMap = playerMap,
            nextActivePlayerUserId = attacker.userId,
            nextStep = CombatStep.Resolution,
        )
    }

    private fun CombatActionArguments.handleTeleportDefenderEscape(): RollOutcome {
        val roll = roll1D6()
        return RollOutcome(
            messages = emptyList(),
            updatedPlayerMap = playerMap,
            nextActivePlayerUserId = attacker.userId,
            nextStep =
                CombatStep.AbilityRoll(
                    purpose = RollPurpose.TeleportAttackerChase(defenderRoll = roll)
                ),
        )
    }

    private fun CombatActionArguments.handleTeleportAttackerChase(defenderRoll: Int): RollOutcome {
        val attackerRoll = roll1D6()
        val escaped = defenderRoll > attackerRoll

        val messages =
            mutableListOf<OutgoingMessage>(
                OutgoingMessage.Teleport(
                    mageUserId = defender.userId,
                    attackerUserId = attacker.userId,
                    escaped = escaped,
                )
            )

        return if (escaped) {
            resolveTurnEnd(extraMessages = messages)
        } else {
            RollOutcome(
                messages = messages,
                updatedPlayerMap = playerMap,
                nextActivePlayerUserId = attacker.userId,
                nextStep = CombatStep.HealthRoll,
            )
        }
    }

    private fun CombatActionArguments.handleRiposteCounter(): RollOutcome {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie
        val triggered = total in 6..8

        val messages =
            mutableListOf<OutgoingMessage>(
                OutgoingMessage.Riposte(
                    duelistUserId = defender.userId,
                    attackerUserId = attacker.userId,
                    triggered = triggered,
                )
            )

        return if (triggered) {
            // Flip attack: Defender now attacks Attacker
            RollOutcome(
                messages = messages,
                updatedPlayerMap = playerMap,
                nextActivePlayerUserId = defender.userId,
                nextStep = CombatStep.ArmorClassRoll,
            )
        } else {
            RollOutcome(
                messages = messages,
                updatedPlayerMap = playerMap,
                nextActivePlayerUserId = attacker.userId,
                nextStep = CombatStep.HealthRoll,
            )
        }
    }

    private fun CombatActionArguments.handlePersuadeStopOrRedirect(): RollOutcome {
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
                    bardUserId = defender.userId,
                    attackerUserId = attacker.userId,
                    outcome = outcome,
                )
            )

        return when (outcome) {
            PersuadeOutcome.Stopped -> resolveTurnEnd(extraMessages = messages)

            PersuadeOutcome.Redirected -> {
                RollOutcome(
                    messages = messages,
                    updatedPlayerMap = playerMap,
                    nextActivePlayerUserId = attacker.userId,
                    nextStep = CombatStep.Resolution, // Attacker needs to pick new target
                )
            }

            PersuadeOutcome.Failed -> {
                RollOutcome(
                    messages = messages,
                    updatedPlayerMap = playerMap,
                    nextActivePlayerUserId = attacker.userId,
                    nextStep = CombatStep.HealthRoll,
                )
            }
        }
    }

    private fun CombatActionArguments.handleLuckyCoinArmorBonus(): RollOutcome {
        val roll = roll1D6()
        val updatedPlayerMap = playerMap.toMutableMap()

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
                        attackerUserId = attacker.userId,
                        armorBonus = roll,
                        reflectDamage = 0,
                    )
                ),
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = attacker.userId,
            nextStep = CombatStep.HealthRoll,
        )
    }

    private fun CombatActionArguments.handleLuckyCoinReflect(): RollOutcome {
        val damage = roll1D6()
        val updatedPlayerMap = playerMap.toMutableMap()

        val newHp = (attacker.characterClass.hitPoints - damage).coerceAtLeast(minimumValue = 0)
        updatedPlayerMap[attacker.userId] =
            attacker.copy(characterClass = attacker.characterClass.copy(hitPoints = newHp))

        val messages =
            listOf(
                OutgoingMessage.LuckyCoin(
                    gamblerUserId = defender.userId,
                    attackerUserId = attacker.userId,
                    armorBonus = 6, // We know it was a 6 to get here
                    reflectDamage = damage,
                )
            )

        return RollOutcome(
            messages = messages,
            updatedPlayerMap = updatedPlayerMap,
            nextActivePlayerUserId = attacker.userId,
            nextStep = CombatStep.HealthRoll,
        )
    }

    private fun CombatActionArguments.handleAllInOutcome(): RollOutcome {
        val roll = roll1D6()
        val outcome =
            when (roll) {
                1 -> AllInOutcome.KnockoutSelf

                5,
                6 -> AllInOutcome.DoubleAttack

                else -> AllInOutcome.NoEffect
            }

        val updatedPlayerMap = playerMap.toMutableMap()
        val messages =
            mutableListOf<OutgoingMessage>(
                OutgoingMessage.AllIn(gamblerUserId = attacker.userId, outcome = outcome)
            )

        return when (outcome) {
            AllInOutcome.KnockoutSelf -> {
                updatedPlayerMap[attacker.userId] =
                    attacker.copy(characterClass = attacker.characterClass.copy(hitPoints = 0))
                resolveTurnEnd(updatedMap = updatedPlayerMap, extraMessages = messages)
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

    private fun CombatActionArguments.handleSummonMinionRoll(): RollOutcome {
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

        val updatedPlayerMap = playerMap.toMutableMap()
        val necro = attacker
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

        return resolveTurnEnd(updatedMap = updatedPlayerMap, extraMessages = messages)
    }

    private fun CombatActionArguments.handlePrepareAttackRoll(): RollOutcome {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie

        var success = total in 10..12

        // Paladin special rule: 12 stops Duelist's Prepare Attack.
        if (total == 12 && defenderDefensiveAbility is DefensiveAbility.HolyShield) {
            success = false
        }

        val updatedPlayerMap = playerMap.toMutableMap()

        if (success) {
            updatedPlayerMap[defender.userId] =
                defender.copy(characterClass = defender.characterClass.copy(hitPoints = 0))
        }

        val messages =
            listOf(
                OutgoingMessage.PrepareAttack(
                    duelistUserId = attacker.userId,
                    targetUserId = defender.userId,
                    success = success,
                )
            )

        return resolveTurnEnd(updatedMap = updatedPlayerMap, extraMessages = messages)
    }

    private fun CombatActionArguments.handleEncoreCheck(): RollOutcome {
        val roll = roll1D6()
        val triggered = roll in 5..6

        val messages =
            listOf(OutgoingMessage.Encore(bardUserId = attacker.userId, triggered = triggered))

        return if (triggered) {
            RollOutcome(
                messages = messages,
                updatedPlayerMap = playerMap,
                nextActivePlayerUserId = attacker.userId,
                nextStep = CombatStep.Resolution, // Attack again
            )
        } else {
            resolveTurnEnd(extraMessages = messages)
        }
    }

    private fun CombatActionArguments.handleHolyShieldSurvival(): RollOutcome {
        val firstDie = roll1D6()
        val secondDie = roll1D6()
        val total = firstDie + secondDie
        val survived = total in 10..12

        val updatedPlayerMap = playerMap.toMutableMap()
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
            resolveTurnEnd(updatedMap = updatedPlayerMap, extraMessages = messages)
        }
    }

    private fun CombatActionArguments.handleHolyShieldRetaliation(): RollOutcome {
        val damage = roll1D6()
        val updatedPlayerMap = playerMap.toMutableMap()

        val newHp = (attacker.characterClass.hitPoints - damage).coerceAtLeast(minimumValue = 0)
        updatedPlayerMap[attacker.userId] =
            attacker.copy(characterClass = attacker.characterClass.copy(hitPoints = newHp))

        val messages =
            listOf(
                OutgoingMessage.HolyShield(
                    paladinUserId = defender.userId,
                    survived = true,
                    retaliationDamage = damage,
                )
            )

        return resolveTurnEnd(updatedMap = updatedPlayerMap, extraMessages = messages)
    }

    private fun CombatActionArguments.handleResolution(): RollOutcome {
        // Usually transitions to AC roll for the attacker
        return RollOutcome(
            messages = emptyList(),
            updatedPlayerMap = playerMap,
            nextActivePlayerUserId = attacker.userId,
            nextStep = CombatStep.ArmorClassRoll,
        )
    }

    private fun CombatActionArguments.resolveTurnEnd(
        updatedMap: Map<String, ArenaPlayer.Selected>? = null,
        extraMessages: List<OutgoingMessage> = emptyList(),
    ): RollOutcome {
        val mapToUse = updatedMap ?: playerMap
        val tempUpdatedMap = mapToUse.toMutableMap()

        // Mark current attacker as acted
        val attackerPlayer = tempUpdatedMap[attacker.userId]
        if (attackerPlayer != null) {
            tempUpdatedMap[attacker.userId] =
                attackerPlayer.copy(
                    characterClass = attackerPlayer.characterClass.copy(hasActedThisRound = true)
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
            messages.add(element = OutgoingMessage.NewRound(roundNumber = roundNumber + 1))

            val fastest =
                tempUpdatedMap.values
                    .filter { it.characterClass.hitPoints > 0 }
                    .maxByOrNull { it.characterClass.speed } ?: attacker

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
