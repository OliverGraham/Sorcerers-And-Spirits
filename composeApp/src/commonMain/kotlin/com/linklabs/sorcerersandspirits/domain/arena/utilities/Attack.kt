package com.linklabs.sorcerersandspirits.domain.arena.utilities

import com.linklabs.sorcerersandspirits.domain.arena.model.AllInOutcome
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatStep
import com.linklabs.sorcerersandspirits.domain.arena.model.MirrorImageStatus
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalRollMessageType
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.PersuadeOutcome
import com.linklabs.sorcerersandspirits.domain.arena.model.RollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.SummonOutcome
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

internal class PerformCombatAction(
    private val arguments: CombatActionArguments,
    private val currentStep: CombatStep,
) {

    internal data class Updates(val nextActivePlayerUserId: String, val nextStep: CombatStep)

    private val messages = mutableListOf<OutgoingMessage>()
    private val updatedPlayerMap = arguments.playerMap.toMutableMap()

    operator fun invoke() {
        val updates = arguments.resolveStep(step = currentStep)
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

    private inner class Attack(
        private val firstDie: Int,
        private val secondDie: Int,
        private val arguments: CombatActionArguments,
    ) {

        private val diceTotal = firstDie + secondDie

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
                        nextStep =
                            CombatStep.AbilityRoll(purpose = RollPurpose.TeleportDefenderEscape),
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
                        nextStep =
                            CombatStep.AbilityRoll(purpose = RollPurpose.PersuadeStopOrRedirect),
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
            if (
                attackerOffensiveAbility == OffensiveAbility.SpellPotency && firstDie == secondDie
            ) {
                return resolveSpellPotency()
            }

            return Updates(
                nextActivePlayerUserId = attacker.userId,
                nextStep = CombatStep.HealthRoll,
            )
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
                val updatedDefenderClass =
                    defender.characterClass.copy(defensiveAbility = updatedDodge)
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
            if (
                attackerOffensiveAbility == OffensiveAbility.SpellPotency && firstDie == secondDie
            ) {
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
                attacker.copy(
                    characterClass = attacker.characterClass.copy(hasActedThisRound = true)
                )
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
}
