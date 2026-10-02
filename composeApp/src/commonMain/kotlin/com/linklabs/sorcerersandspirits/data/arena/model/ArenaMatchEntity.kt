package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ArenaMatchEntity {
    val newMessages: List<ArenaMessageEntity>
    val playerEntitiesMap: Map<String, ArenaPlayerEntity>
    val matchNumberInGauntlet: Int

    @Serializable
    @SerialName(value = WAITING_FOR_ALL_PLAYERS)
    data class WaitingForAllPlayers(
        override val playerEntitiesMap: Map<String, ArenaPlayerEntity> = emptyMap(),
        override val newMessages: List<ArenaMessageEntity> = emptyList(),
        override val matchNumberInGauntlet: Int = 1,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = CHOOSING_CLASSES)
    data class ChoosingClasses(
        override val newMessages: List<ArenaMessageEntity>,
        override val playerEntitiesMap: Map<String, ArenaPlayerEntity>,
        override val matchNumberInGauntlet: Int = 1,
        val activePlayerUserId: String,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = COMBAT)
    data class Combat(
        override val newMessages: List<ArenaMessageEntity>,
        override val playerEntitiesMap: Map<String, ArenaPlayerEntity.Selected>,
        override val matchNumberInGauntlet: Int = 1,
        val nextCombatActionEntity: NextCombatActionEntity,
        val roundNumber: Int,
        val isNewRound: Boolean,
        val isMatchOver: Boolean = false,
        val diceRoll: DiceRollEntity? = null,
        val currentTargetUserId: String? = null,
        val participantEntitiesMap: Map<String, MatchParticipantEntity>,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = FINISHED)
    data class Finished(
        override val newMessages: List<ArenaMessageEntity>,
        override val playerEntitiesMap: Map<String, ArenaPlayerEntity>,
        override val matchNumberInGauntlet: Int = 1,
        val participantEntitiesMap: Map<String, MatchParticipantEntity>,
    ) : ArenaMatchEntity

    companion object {
        private const val WAITING_FOR_ALL_PLAYERS = "WaitingForAllPlayers"
        private const val CHOOSING_CLASSES = "ChoosingClasses"
        private const val COMBAT = "Combat"
        private const val FINISHED = "Finished"

        val playerEntitiesMapName = ChoosingClasses::playerEntitiesMap.name
        val newMessagesName = ArenaMatchEntity::newMessages.name
    }
}

internal fun ArenaMatchEntity.toDomain(): ArenaMatch {
    val messages = newMessages.toDomain()

    return when (this) {
        is ArenaMatchEntity.WaitingForAllPlayers ->
            ArenaMatch.WaitingForAllPlayers(
                messages = messages,
                playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
                matchNumberInGauntlet = matchNumberInGauntlet,
            )

        is ArenaMatchEntity.ChoosingClasses -> {
            val playerMap = playerEntitiesMap.mapValues { it.value.toDomain() }

            ArenaMatch.ChoosingClasses(
                playerMap = playerMap,
                messages = messages,
                activePlayerUserId = activePlayerUserId,
                activePlayer = playerMap.getValue(key = activePlayerUserId),
                matchNumberInGauntlet = matchNumberInGauntlet,
            )
        }

        is ArenaMatchEntity.Combat ->
            ArenaMatch.Combat(
                playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
                messages = messages,
                nextCombatAction = nextCombatActionEntity.toDomain(),
                currentTargetUserId = currentTargetUserId,
                roundNumber = roundNumber,
                isNewRound = isNewRound,
                isMatchOver = isMatchOver,
                matchNumberInGauntlet = matchNumberInGauntlet,
                diceRoll = diceRoll?.toDomain(),
                participantMap = participantEntitiesMap.toDomain(),
            )

        is ArenaMatchEntity.Finished ->
            ArenaMatch.Finished(
                messages = messages,
                playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
                matchNumberInGauntlet = matchNumberInGauntlet,
                participantMap = participantEntitiesMap.toDomain(),
            )
    }
}
