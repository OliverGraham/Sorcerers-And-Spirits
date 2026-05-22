package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatResult
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import com.linklabs.sorcerersandspirits.domain.arena.model.TurnInfo
import com.linklabs.sorcerersandspirits.domain.arena.utilities.roll1D6
import com.linklabs.sorcerersandspirits.domain.game.ActivatedAbility
import com.linklabs.sorcerersandspirits.domain.game.Anticipation
import com.linklabs.sorcerersandspirits.domain.game.Blink
import com.linklabs.sorcerersandspirits.domain.game.HolyPrayer
import com.linklabs.sorcerersandspirits.domain.game.KillingMomentum
import com.linklabs.sorcerersandspirits.domain.game.PrepareAnAttack
import com.linklabs.sorcerersandspirits.domain.game.SpellNova
import kotlin.random.Random

data class CombatActionArguments(
    val arenaId: String,
    val attacker: ArenaPlayer.Selected,
    val defender: ArenaPlayer.Selected,
    val playerMap: Map<String, ArenaPlayer.Selected>,
    val turnInfo: TurnInfo,
) {
    val attackerOffensiveAbility = attacker.characterClass.offensiveAbility
    val attackerDefensiveAbility = attacker.characterClass.defensiveAbility
    val defenderOffensiveAbility = attacker.characterClass.offensiveAbility
    val defenderDefensiveAbility = attacker.characterClass.defensiveAbility
}

// TODO Arena: Clean this slop - need to reduce number of arguments. An idea is map to CombatResult,
//  and then make the list of messages based on that, instead of that horrible mix.
class PerformCombatActionUseCase(private val arenaRepository: ArenaRepository) {

    suspend operator fun invoke(arguments: CombatActionArguments, newRollType: RollType) {
        arguments.handleRoll(newRollType = newRollType)
    }

    private suspend fun CombatActionArguments.handleRoll(newRollType: RollType) {
        if (newRollType is RollType.AbilityRoll) {
            return handleAbilityRoll(rollType = newRollType)
        }

        // TODO: What about the possible +1 from doubles from the previous roll?

        val firstDie = roll1D6()
        val secondDie = roll1D6()

        if (attackerOffensiveAbility == SpellNova) {
            if (firstDie == secondDie) {
                return // TODO handle SpellNova
            }
        }

        when (newRollType) {
            is RollType.ArmorClassRoll ->
                handleArmorClassRoll(firstDie = firstDie, secondDie = secondDie)

            is RollType.HealthRoll -> handleHealthRoll(firstDie = firstDie, secondDie = secondDie)
        }
    }

    // TODO: In future, can non-Activated abilities be handlined in this manner?
    private fun CombatActionArguments.handleAbilityRoll(rollType: RollType.AbilityRoll) {
        val activatedAbility = rollType.ability as? ActivatedAbility ?: return

        when (activatedAbility) {
            PrepareAnAttack -> {
                // TODO: I think it's roll a 10, 11 or 12 to instant-kill opponent?
            }
        }
    }

    private fun CombatActionArguments.handleArmorClassRoll(firstDie: Int, secondDie: Int) {
        if (firstDie + secondDie >= defender.characterClass.armorClass) {
            resolveArmorClassHit(firstDie = firstDie, secondDie = secondDie)
        } else {
            resolveArmorClassMiss(firstDie = firstDie, secondDie = secondDie)
        }
    }

    private fun CombatActionArguments.resolveArmorClassHit(firstDie: Int, secondDie: Int) {
        when (defenderDefensiveAbility) {
            is Blink -> {
                if (firstDie + secondDie >= defender.characterClass.armorClass) {
                    // TODO: handle Blink
                } else {
                    // TODO: Resolve normal AC hit
                }
            }

            // TODO: Gambler ability (can't remember)
            // TODO: Duelist Riposte
            // TODO: Bard Persuade
            // TODO: Illusionist reroll option (is this truly everywhere?)
            else -> {
                // TODO: Resolve normal AC hit
            }
        }
    }

    private fun CombatActionArguments.resolveArmorClassMiss(firstDie: Int, secondDie: Int) {
        // No other class does anything after a miss to their AC right?
        if (defenderDefensiveAbility is Anticipation) {
            val anticipationCounters = defenderDefensiveAbility.counters
            if (anticipationCounters < 2) {
                // TODO: Add Anticipation counter
            } else {
                // TODO: Add message stating Rogue has max Anticipation
            }
        }

        when (attackerOffensiveAbility) {
            // TODO: Gambler could have another turn
            // TODO: Necro could have another turn
            else -> {}
        }
    }

    private fun CombatActionArguments.handleHealthRoll(firstDie: Int, secondDie: Int) {
        if (firstDie + secondDie >= defender.characterClass.hitPoints) {
            resolveHealthKill(firstDie = firstDie, secondDie = secondDie)
        } else {
            resolveHealthHit(firstDie = firstDie, secondDie = secondDie)
        }

        // TODO: Bard's Encore
    }

    private fun CombatActionArguments.resolveHealthKill(firstDie: Int, secondDie: Int) {
        if (defenderDefensiveAbility == HolyPrayer) {
            // TODO: Handle Paladin bubble - if survive, then come back here and handle regularly
        }

        if (attackerOffensiveAbility == KillingMomentum) {
            // TODO: Handle KillingMomentum - carry +1 from doubles if applicable
        }

        // TODO: otherwise, normal character knock out
    }

    private fun CombatActionArguments.resolveHealthHit(firstDie: Int, secondDie: Int) {
        // No other class does anything after a miss to their AC right?
        if (defenderDefensiveAbility is Anticipation) {
            val anticipationCounters = defenderDefensiveAbility.counters
            if (anticipationCounters < 2) {
                // TODO: Add Anticipation counter
            } else {
                // TODO: Add message stating Rogue has max Anticipation
            }
        }

        when (attackerOffensiveAbility) {
            // TODO: Gambler could have another turn
            // TODO: Necro could have another turn
            else -> {}
        }
    }

    suspend operator fun invoke(
        arenaId: String,
        playerMap: Map<String, ArenaPlayer.Selected>,
        attackerUserId: String,
        targetUserId: String,
        newRollType: RollType,
        turnInfo: TurnInfo,
    ) {
        // TODO: Need to use two different dice
        val diceResult = Random.nextInt(from = 1, until = 7) + Random.nextInt(from = 1, until = 7)

        val attacker = playerMap[attackerUserId] ?: return
        val target = playerMap[targetUserId] ?: return

        val newMessages = mutableListOf<OutgoingArenaMessage>()

        newMessages.add(
            element =
                OutgoingArenaMessage.DiceRoll(
                    rollerClassName = attacker.characterClass.className,
                    rollValue = diceResult,
                    rollType = newRollType,
                )
        )

        val result =
            when (newRollType) {
                RollType.ArmorClassRoll ->
                    handleArmorClassRoll(
                        turnInfo = turnInfo,
                        playerMap = playerMap,
                        diceResult = diceResult,
                        attacker = attacker,
                        target = target,
                        newMessages = newMessages,
                    )

                RollType.HealthRoll ->
                    handleHealthRoll(
                        turnInfo = turnInfo,
                        playerMap = playerMap,
                        diceResult = diceResult,
                        attacker = attacker,
                        target = target,
                        newMessages = newMessages,
                    )

                is RollType.AbilityRoll -> return
            }

        arenaRepository.applyCombatResult(arenaId = arenaId, result = result)
    }

    private fun handleArmorClassRoll(
        turnInfo: TurnInfo,
        playerMap: Map<String, ArenaPlayer.Selected>,
        diceResult: Int,
        attacker: ArenaPlayer.Selected,
        target: ArenaPlayer.Selected,
        newMessages: MutableList<OutgoingArenaMessage>,
    ): CombatResult {
        val isHit = diceResult >= target.characterClass.armorClass

        newMessages.add(
            element =
                OutgoingArenaMessage.AttackResult(
                    attackerClassName = attacker.characterClass.className,
                    targetClassName = target.characterClass.className,
                    isHit = isHit,
                    rollType = RollType.ArmorClassRoll,
                )
        )

        if (isHit) {
            return CombatResult.Hit(
                messages = newMessages,
                targetId = target.userId,
                nextTurnInfo = null,
            )
        }

        val turnAdvance = determineNextTurnInfo(turnInfo = turnInfo, playerMap = playerMap)

        addTurnMessages(messages = newMessages, playerMap = playerMap, turnAdvance = turnAdvance)

        return CombatResult.Miss(messages = newMessages, nextTurnInfo = turnAdvance.turnInfo)
    }

    private fun handleHealthRoll(
        turnInfo: TurnInfo,
        playerMap: Map<String, ArenaPlayer.Selected>,
        diceResult: Int,
        attacker: ArenaPlayer.Selected,
        target: ArenaPlayer.Selected,
        newMessages: MutableList<OutgoingArenaMessage>,
    ): CombatResult {
        val isDead = diceResult >= target.characterClass.hitPoints

        newMessages.add(
            element =
                OutgoingArenaMessage.AttackResult(
                    attackerClassName = attacker.characterClass.className,
                    targetClassName = target.characterClass.className,
                    isHit = isDead,
                    rollType = RollType.HealthRoll,
                )
        )

        return if (isDead) {
            val updatedPlayerMap = playerMap.toMutableMap()
            updatedPlayerMap.remove(target.userId)

            newMessages.add(
                element =
                    OutgoingArenaMessage.TargetDead(
                        targetClassName = target.characterClass.className,
                        killerClassName = attacker.characterClass.className,
                    )
            )

            if (updatedPlayerMap.size <= 1) {
                val winner = updatedPlayerMap.values.firstOrNull()
                if (winner != null) {
                    newMessages.add(
                        OutgoingArenaMessage.GameFinished(
                            winnerClassName = winner.characterClass.className
                        )
                    )
                }

                CombatResult.Victory(
                    messages = newMessages,
                    winnerClassName = winner?.characterClass?.className ?: "Unknown",
                )
            } else {
                val turnAdvance =
                    determineNextTurnInfo(turnInfo = turnInfo, playerMap = updatedPlayerMap)

                addTurnMessages(
                    messages = newMessages,
                    playerMap = updatedPlayerMap,
                    turnAdvance = turnAdvance,
                )

                CombatResult.TargetDefeated(
                    messages = newMessages,
                    defeatedPlayerId = target.userId,
                    nextTurnInfo = turnAdvance.turnInfo,
                )
            }
        } else {
            val newHp = target.characterClass.hitPoints - diceResult

            val turnAdvance = determineNextTurnInfo(turnInfo = turnInfo, playerMap = playerMap)

            addTurnMessages(
                messages = newMessages,
                playerMap = playerMap,
                turnAdvance = turnAdvance,
            )

            CombatResult.DamageDealt(
                messages = newMessages,
                targetId = target.userId,
                newHp = newHp,
                nextTurnInfo = turnAdvance.turnInfo,
            )
        }
    }

    private fun addTurnMessages(
        messages: MutableList<OutgoingArenaMessage>,
        playerMap: Map<String, ArenaPlayer.Selected>,
        turnAdvance: TurnAdvance,
    ) {
        if (turnAdvance.isNewRound) {
            messages.add(
                element =
                    OutgoingArenaMessage.NewRound(roundNumber = turnAdvance.turnInfo.roundNumber)
            )
        }

        messages.add(
            element =
                generateNewTurnMessage(
                    playerMap = playerMap,
                    nextPlayerId = turnAdvance.turnInfo.activePlayerUserId,
                )
        )
    }

    private fun generateNewTurnMessage(
        playerMap: Map<String, ArenaPlayer.Selected>,
        nextPlayerId: String,
    ): OutgoingArenaMessage {
        val nextPlayer = playerMap.getValue(key = nextPlayerId)

        return OutgoingArenaMessage.NewTurn(
            characterClassInfo =
                CharacterClassMessageInfo(
                    className = nextPlayer.characterClass.className,
                    type = nextPlayer.characterClass.type,
                )
        )
    }

    private fun determineNextTurnInfo(
        turnInfo: TurnInfo,
        playerMap: Map<String, ArenaPlayer.Selected>,
    ): TurnAdvance {
        val updatedActedThisRoundUserIds =
            turnInfo.actedThisRoundUserIds + turnInfo.activePlayerUserId

        val isNewRound = playerMap.keys.all { playerId -> playerId in updatedActedThisRoundUserIds }

        return if (isNewRound) {
            val nextActivePlayer = playerMap.values.maxBy { player -> player.characterClass.speed }

            TurnAdvance(
                turnInfo =
                    TurnInfo(
                        activePlayerUserId = nextActivePlayer.userId,
                        actedThisRoundUserIds = emptySet(),
                        roundNumber = turnInfo.roundNumber + 1,
                    ),
                isNewRound = true,
            )
        } else {
            val nextActivePlayer =
                playerMap.values
                    .filter { player -> player.userId !in updatedActedThisRoundUserIds }
                    .maxBy { player -> player.characterClass.speed }

            TurnAdvance(
                turnInfo =
                    TurnInfo(
                        activePlayerUserId = nextActivePlayer.userId,
                        actedThisRoundUserIds = updatedActedThisRoundUserIds,
                        roundNumber = turnInfo.roundNumber,
                    ),
                isNewRound = false,
            )
        }
    }

    private data class TurnAdvance(val turnInfo: TurnInfo, val isNewRound: Boolean)
}
