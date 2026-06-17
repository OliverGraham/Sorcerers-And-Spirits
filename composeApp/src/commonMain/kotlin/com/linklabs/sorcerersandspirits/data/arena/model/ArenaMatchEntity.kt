package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ArenaMatchEntity {
    val messages: List<ArenaMessageEntity>
    val numberOfNewMessages: Int
    val playerEntitiesMap: Map<String, ArenaPlayerEntity>
    val matchNumberInGauntlet: Int

    @Serializable
    @SerialName(value = WAITING_FOR_ALL_PLAYERS)
    data class WaitingForAllPlayers(
        override val playerEntitiesMap: Map<String, ArenaPlayerEntity> = emptyMap(),
        override val messages: List<ArenaMessageEntity> = emptyList(),
        override val numberOfNewMessages: Int = 0,
        override val matchNumberInGauntlet: Int = 1,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = CHOOSING_CLASSES)
    data class ChoosingClasses(
        override val messages: List<ArenaMessageEntity>,
        override val numberOfNewMessages: Int,
        override val playerEntitiesMap: Map<String, ArenaPlayerEntity>,
        val activePlayerUserId: String,
        override val matchNumberInGauntlet: Int = 1,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = COMBAT)
    data class Combat(
        override val messages: List<ArenaMessageEntity>,
        override val numberOfNewMessages: Int,
        override val playerEntitiesMap: Map<String, ArenaPlayerEntity.Selected>,
        val nextCombatActionEntity: NextCombatActionEntity,
        val roundNumber: Int,
        val isNewRound: Boolean,
        val doublesBonusEntity: DoublesBonusEntity? = null,
        override val matchNumberInGauntlet: Int = 1,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = FINISHED)
    data class Finished(
        override val messages: List<ArenaMessageEntity>,
        override val numberOfNewMessages: Int,
        override val playerEntitiesMap: Map<String, ArenaPlayerEntity>,
        override val matchNumberInGauntlet: Int = 1,
    ) : ArenaMatchEntity

    companion object {
        private const val WAITING_FOR_ALL_PLAYERS = "WaitingForAllPlayers"
        private const val CHOOSING_CLASSES = "ChoosingClasses"
        private const val COMBAT = "Combat"
        private const val FINISHED = "Finished"

        val playerEntitiesMapName = ChoosingClasses::playerEntitiesMap.name
        val messagesName = ArenaMatchEntity::messages.name
    }
}

internal fun ArenaMatchEntity.toDomain(): ArenaMatch {
    val messages = messages.map { it.toDomain() }

    return when (this) {
        is ArenaMatchEntity.WaitingForAllPlayers ->
            ArenaMatch.WaitingForAllPlayers(
                messages = messages,
                numberOfNewMessages = numberOfNewMessages,
                playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
                matchNumberInGauntlet = matchNumberInGauntlet,
            )

        is ArenaMatchEntity.ChoosingClasses -> {
            val playerMap = playerEntitiesMap.mapValues { it.value.toDomain() }

            ArenaMatch.ChoosingClasses(
                playerMap = playerMap,
                messages = messages,
                numberOfNewMessages = numberOfNewMessages,
                activePlayerUserId = activePlayerUserId,
                activePlayer = playerMap.getValue(key = activePlayerUserId),
                matchNumberInGauntlet = matchNumberInGauntlet,
            )
        }

        is ArenaMatchEntity.Combat ->
            ArenaMatch.Combat(
                playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
                messages = messages,
                numberOfNewMessages = numberOfNewMessages,
                nextCombatAction = nextCombatActionEntity.toDomain(),
                roundNumber = roundNumber,
                isNewRound = isNewRound,
                doublesBonus = doublesBonusEntity?.toDomain(),
                matchNumberInGauntlet = matchNumberInGauntlet,
            )

        is ArenaMatchEntity.Finished ->
            ArenaMatch.Finished(
                messages = messages,
                numberOfNewMessages = numberOfNewMessages,
                playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
                matchNumberInGauntlet = matchNumberInGauntlet,
            )
    }
}
