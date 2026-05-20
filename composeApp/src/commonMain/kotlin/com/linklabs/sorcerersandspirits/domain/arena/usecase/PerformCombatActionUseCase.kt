package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatResult
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import com.linklabs.sorcerersandspirits.domain.arena.model.TurnInfo
import kotlin.random.Random

// TODO Arena: Clean this slop - need to reduce number of arguments. An idea is map to CombatResult,
//  and then make the list of messages based on that, instead of that horrible mix.
class PerformCombatActionUseCase(private val arenaRepository: ArenaRepository) {

    suspend operator fun invoke(
        arenaId: String,
        playerMap: Map<String, ArenaPlayer.Selected>,
        attackerUserId: String,
        targetUserId: String,
        rollType: RollType,
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
                    rollType = rollType,
                )
        )

        val result =
            when (rollType) {
                RollType.ArmorClass ->
                    handleArmorClassRoll(
                        turnInfo = turnInfo,
                        playerMap = playerMap,
                        diceResult = diceResult,
                        attacker = attacker,
                        target = target,
                        newMessages = newMessages,
                    )

                RollType.Health ->
                    handleHealthRoll(
                        turnInfo = turnInfo,
                        playerMap = playerMap,
                        diceResult = diceResult,
                        attacker = attacker,
                        target = target,
                        newMessages = newMessages,
                    )

                RollType.Ability -> return
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
                    rollType = RollType.ArmorClass,
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
                    rollType = RollType.Health,
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
