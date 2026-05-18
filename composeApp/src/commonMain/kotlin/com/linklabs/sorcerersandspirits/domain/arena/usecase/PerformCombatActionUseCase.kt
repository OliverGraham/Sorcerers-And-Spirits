package com.linklabs.sorcerersandspirits.domain.arena.usecase

import com.linklabs.sorcerersandspirits.domain.arena.ArenaRepository
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.CharacterClassMessageInfo
import com.linklabs.sorcerersandspirits.domain.arena.model.CombatResult
import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import kotlin.random.Random

// TODO Arena: Clean this slop
class PerformCombatActionUseCase(private val arenaRepository: ArenaRepository) {
    suspend operator fun invoke(
        arenaId: String,
        activePlayerUserId: String,
        playerMap: Map<String, ArenaPlayer.Selected>,
        attackerUserId: String,
        targetUserId: String,
        rollType: RollType,
        messageStartIndex: Int,
    ) {
        if (activePlayerUserId != attackerUserId) return

        val diceResult = Random.nextInt(from = 1, until = 7) + Random.nextInt(from = 1, until = 7)
        val attacker = playerMap[attackerUserId] ?: return
        val target = playerMap[targetUserId] ?: return

        val newMessages = mutableListOf<OutgoingArenaMessage>()

        newMessages.add(
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
                        activePlayerUserId = activePlayerUserId,
                        playerMap = playerMap,
                        diceResult = diceResult,
                        attacker = attacker,
                        target = target,
                        newMessages = newMessages,
                        messageStartIndex = messageStartIndex,
                    )

                RollType.Health ->
                    handleHealthRoll(
                        activePlayerUserId = activePlayerUserId,
                        playerMap = playerMap,
                        diceResult = diceResult,
                        attacker = attacker,
                        target = target,
                        newMessages = newMessages,
                        messageStartIndex = messageStartIndex,
                    )

                else -> return
            }

        arenaRepository.applyCombatResult(arenaId = arenaId, result = result)
    }

    private fun handleArmorClassRoll(
        activePlayerUserId: String,
        playerMap: Map<String, ArenaPlayer.Selected>,
        diceResult: Int,
        attacker: ArenaPlayer.Selected,
        target: ArenaPlayer.Selected,
        newMessages: MutableList<OutgoingArenaMessage>,
        messageStartIndex: Int,
    ): CombatResult {
        val isHit = diceResult >= target.characterClass.armorClass

        newMessages.add(
            OutgoingArenaMessage.AttackResult(
                attackerClassName = attacker.characterClass.className,
                targetClassName = target.characterClass.className,
                isHit = isHit,
                rollType = RollType.ArmorClass,
            )
        )

        return if (isHit) {
            CombatResult.Hit(
                messages = newMessages,
                messageStartIndex = messageStartIndex,
                targetId = target.userId,
            )
        } else {
            val nextPlayerId =
                determineNextPlayerId(
                    activePlayerUserId = activePlayerUserId,
                    playerMap = playerMap,
                )
            newMessages.add(
                generateNewTurnMessage(playerMap = playerMap, nextPlayerId = nextPlayerId)
            )
            CombatResult.Miss(
                messages = newMessages,
                messageStartIndex = messageStartIndex,
                nextActivePlayerId = nextPlayerId,
            )
        }
    }

    private fun handleHealthRoll(
        activePlayerUserId: String,
        playerMap: Map<String, ArenaPlayer.Selected>,
        diceResult: Int,
        attacker: ArenaPlayer.Selected,
        target: ArenaPlayer.Selected,
        newMessages: MutableList<OutgoingArenaMessage>,
        messageStartIndex: Int,
    ): CombatResult {
        val isDead = diceResult >= target.characterClass.hitPoints

        newMessages.add(
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
                    messageStartIndex = messageStartIndex,
                    winnerClassName = winner?.characterClass?.className ?: "Unknown",
                )
            } else {
                val nextPlayerId =
                    determineNextPlayerId(
                        activePlayerUserId = activePlayerUserId,
                        playerMap = updatedPlayerMap,
                    )
                newMessages.add(
                    generateNewTurnMessage(
                        playerMap = updatedPlayerMap,
                        nextPlayerId = nextPlayerId,
                    )
                )
                CombatResult.TargetDefeated(
                    messages = newMessages,
                    messageStartIndex = messageStartIndex,
                    defeatedPlayerId = target.userId,
                    nextActivePlayerId = nextPlayerId,
                )
            }
        } else {
            val newHp = target.characterClass.hitPoints - diceResult
            val nextPlayerId =
                determineNextPlayerId(
                    activePlayerUserId = activePlayerUserId,
                    playerMap = playerMap,
                )
            newMessages.add(
                generateNewTurnMessage(playerMap = playerMap, nextPlayerId = nextPlayerId)
            )
            CombatResult.DamageDealt(
                messages = newMessages,
                messageStartIndex = messageStartIndex,
                targetId = target.userId,
                newHp = newHp,
                nextActivePlayerId = nextPlayerId,
            )
        }
    }

    private fun determineNextPlayerId(
        activePlayerUserId: String,
        playerMap: Map<String, ArenaPlayer.Selected>,
    ): String =
        playerMap.values
            .filter { player -> player.userId != activePlayerUserId }
            .maxBy { player -> player.characterClass.speed }
            .userId

    private fun generateNewTurnMessage(
        playerMap: Map<String, ArenaPlayer.Selected>,
        nextPlayerId: String,
    ): OutgoingArenaMessage {
        val nextPlayer = playerMap[nextPlayerId]!!
        return OutgoingArenaMessage.NewTurn(
            characterClassInfo =
                CharacterClassMessageInfo(
                    className = nextPlayer.characterClass.className,
                    type = nextPlayer.characterClass.type,
                )
        )
    }
}
