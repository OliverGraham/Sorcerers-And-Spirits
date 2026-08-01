package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ArenaMatchEntity {
    // CHANGE: `messages` now holds ONLY the latest batch written to RTDB (replace semantics), not a
    // rolling log. Delete `numberOfNewMessages` from the interface and every subtype below; the
    // batch size is just messages.size, and "how many are new" is no longer an RTDB concern.
    // Yes, although here we can now call the property newMessages (not messages)
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
        override val matchNumberInGauntlet: Int = 1,
        val activePlayerUserId: String,
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = COMBAT)
    data class Combat(
        override val messages: List<ArenaMessageEntity>,
        override val numberOfNewMessages: Int,
        override val playerEntitiesMap: Map<String, ArenaPlayerEntity.Selected>,
        override val matchNumberInGauntlet: Int = 1,
        val nextCombatActionEntity: NextCombatActionEntity,
        val roundNumber: Int,
        val isNewRound: Boolean,
        val isMatchOver: Boolean = false,
        val diceRoll: DiceRollEntity? = null,
        val participants: Map<String, CharacterClassType> = emptyMap(),
    ) : ArenaMatchEntity

    @Serializable
    @SerialName(value = FINISHED)
    data class Finished(
        override val messages: List<ArenaMessageEntity>,
        override val numberOfNewMessages: Int,
        override val playerEntitiesMap: Map<String, ArenaPlayerEntity>,
        override val matchNumberInGauntlet: Int = 1,
        val participants: Map<String, CharacterClassType> = emptyMap(),
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

// TODO: Double check this. The mapping should be able to be done without any filtering
internal fun Map<String, ArenaPlayerEntity>.toParticipants(): Map<String, CharacterClassType> =
    values.filterIsInstance<ArenaPlayerEntity.Selected>().associate {
        it.userId to it.characterClassEntity.type
    }

// CHANGE: this maps the RTDB entity -> domain ArenaMatch, where `messages` is now the latest BATCH.
// The domain ArenaMatch gains `newMessages` (this batch) and its `messages` (full log) is filled in
// downstream by CoordinateArenaMatchUseCase from the on-device store — NOT here.
// QUESTION: should toDomain set ArenaMatch.messages = this batch as a placeholder (use case
// overwrites with the full log), or should the use case be the ONLY place messages is assembled and
// toDomain leave messages empty + populate newMessages only? Leaning: latter (single owner).
// Yes, but call it newMessages so it'll be easy and clear. Since we're in the data layer, the
//  local data source should be combined here, before going to the Domain layer. I had the wrong-ish
//  wording when first describing what to do. So, there should be an interception before calling toDomain()
//  where it gets the local data source and combines with the new messages. Hmm, is that the correct
//  way? I don't want to construct an ArenaMatch instance here with empty messages - that's a code smell;
//  so I'm trying to think of a good way around that.
internal fun ArenaMatchEntity.toDomain(): ArenaMatch {
    val messages = messages.toDomain()

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
                isMatchOver = isMatchOver,
                matchNumberInGauntlet = matchNumberInGauntlet,
                diceRoll = diceRoll?.toDomain(),
                participants = participants,
            )

        is ArenaMatchEntity.Finished ->
            ArenaMatch.Finished(
                messages = messages,
                numberOfNewMessages = numberOfNewMessages,
                playerMap = playerEntitiesMap.mapValues { it.value.toDomain() },
                matchNumberInGauntlet = matchNumberInGauntlet,
                participants = participants,
            )
    }
}
