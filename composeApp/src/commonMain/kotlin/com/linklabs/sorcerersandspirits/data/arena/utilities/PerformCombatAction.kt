package com.linklabs.sorcerersandspirits.data.arena.utilities

import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMatchEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaMessageEntity
import com.linklabs.sorcerersandspirits.data.arena.model.ArenaPlayerEntity
import com.linklabs.sorcerersandspirits.data.arena.model.CharacterClassMessageInfoEntity
import com.linklabs.sorcerersandspirits.domain.arena.model.RollType
import dev.gitlive.firebase.database.DatabaseReference
import kotlin.random.Random

class PerformCombatAction(
    private val arenaReference: DatabaseReference,
    private val attackerUserId: String,
    private val targetUserId: String,
    private val rollType: RollType,
) {
    suspend operator fun invoke() {
        arenaReference.runTransaction(strategy = ArenaMatchEntity.serializer()) { match ->
            match.performCombatActionTransaction()
        }
    }

    private fun ArenaMatchEntity?.performCombatActionTransaction(): ArenaMatchEntity {
        if (this !is ArenaMatchEntity.Combat) return this ?: error("Match not found")
        if (activePlayerUserId != attackerUserId) return this

        val diceResult = Random.nextInt(from = 1, until = 7) + Random.nextInt(from = 1, until = 7)
        val attacker = playerEntitiesMap[attackerUserId] ?: return this
        val target = playerEntitiesMap[targetUserId] ?: return this

        val messagesReference = arenaReference.child(path = ArenaMatchEntity.messagesName)
        val updatedMessages = messages.toMutableList()

        updatedMessages.add(
            ArenaMessageEntity.DiceRoll(
                id = messagesReference.addNewKey(),
                rollerDisplayName = attacker.displayName,
                rollValue = diceResult,
                rollType = rollType,
            )
        )

        return when (rollType) {
            RollType.ArmorClass ->
                handleArmorClassRoll(
                    diceResult = diceResult,
                    attacker = attacker,
                    target = target,
                    updatedMessages = updatedMessages,
                    messagesReference = messagesReference,
                )

            RollType.Health ->
                handleHealthRoll(
                    diceResult = diceResult,
                    attacker = attacker,
                    target = target,
                    updatedMessages = updatedMessages,
                    messagesReference = messagesReference,
                )

            else -> this
        }
    }

    private fun ArenaMatchEntity.Combat.handleArmorClassRoll(
        diceResult: Int,
        attacker: ArenaPlayerEntity.Selected,
        target: ArenaPlayerEntity.Selected,
        updatedMessages: MutableList<ArenaMessageEntity>,
        messagesReference: DatabaseReference,
    ): ArenaMatchEntity {
        val isHit = diceResult >= target.characterClassEntity.armorClass

        updatedMessages.add(
            ArenaMessageEntity.AttackResult(
                id = messagesReference.addNewKey(),
                attackerDisplayName = attacker.displayName,
                targetDisplayName = target.displayName,
                isHit = isHit,
                rollType = RollType.ArmorClass,
            )
        )

        return if (isHit) {
            copy(
                messages = updatedMessages,
                numberOfNewMessages = updatedMessages.size - messages.size,
                pendingRollType = RollType.Health,
                pendingTargetUserId = targetUserId,
            )
        } else {
            endTurn(updatedMessages = updatedMessages, messagesReference = messagesReference)
        }
    }

    private fun ArenaMatchEntity.Combat.handleHealthRoll(
        diceResult: Int,
        attacker: ArenaPlayerEntity.Selected,
        target: ArenaPlayerEntity.Selected,
        updatedMessages: MutableList<ArenaMessageEntity>,
        messagesReference: DatabaseReference,
    ): ArenaMatchEntity {
        val isDead = diceResult >= target.characterClassEntity.hitPoints

        updatedMessages.add(
            ArenaMessageEntity.AttackResult(
                id = messagesReference.addNewKey(),
                attackerDisplayName = attacker.displayName,
                targetDisplayName = target.displayName,
                isHit = isDead,
                rollType = RollType.Health,
            )
        )

        val updatedPlayers = playerEntitiesMap.toMutableMap()
        if (isDead) {
            updatedPlayers.remove(key = targetUserId)
            updatedMessages.add(
                ArenaMessageEntity.TargetDead(
                    id = messagesReference.addNewKey(),
                    targetDisplayName = target.displayName,
                    killerDisplayName = attacker.displayName,
                )
            )
        }

        return copy(playerEntitiesMap = updatedPlayers)
            .endTurn(updatedMessages = updatedMessages, messagesReference = messagesReference)
    }

    private fun ArenaMatchEntity.Combat.endTurn(
        updatedMessages: MutableList<ArenaMessageEntity>,
        messagesReference: DatabaseReference,
    ): ArenaMatchEntity {
        if (playerEntitiesMap.size <= 1) {
            val winner = playerEntitiesMap.values.firstOrNull()
            if (winner != null) {
                updatedMessages.add(
                    ArenaMessageEntity.GameFinished(
                        id = messagesReference.addNewKey(),
                        winnerDisplayName = winner.displayName,
                    )
                )
            }
            return ArenaMatchEntity.Finished(
                messages = updatedMessages,
                numberOfNewMessages = updatedMessages.size - messages.size,
            )
        }

        val currentIndex = turnOrder.indexOf(element = activePlayerUserId)
        var nextIndex = (currentIndex + 1) % turnOrder.size
        while (!playerEntitiesMap.containsKey(key = turnOrder[nextIndex])) {
            nextIndex = (nextIndex + 1) % turnOrder.size
        }
        val nextPlayerId = turnOrder[nextIndex]
        val nextPlayer = playerEntitiesMap[nextPlayerId]!!

        updatedMessages.add(
            ArenaMessageEntity.NewTurn(
                id = messagesReference.addNewKey(),
                characterClassInfo =
                    CharacterClassMessageInfoEntity(
                        className = nextPlayer.characterClassEntity.className,
                        type = nextPlayer.characterClassEntity.type,
                    ),
            )
        )

        return copy(
            activePlayerUserId = nextPlayerId,
            messages = updatedMessages,
            numberOfNewMessages = updatedMessages.size - messages.size,
            pendingRollType = RollType.ArmorClass,
            pendingTargetUserId = null,
        )
    }

    private fun DatabaseReference.addNewKey(): String =
        push().key ?: error("Failed to generate Firebase push key")
}
