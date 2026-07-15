package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.LatestRoll
import com.linklabs.sorcerersandspirits.domain.arena.model.RolledDice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: DELETE

private const val FIRST_DIE_SLOT = 0
private const val SECOND_DIE_SLOT = 1

@Serializable
sealed interface RolledDiceEntity {

    @Serializable
    @SerialName(value = FACES)
    data class Faces(val firstDie: Int, val secondDie: Int? = null) : RolledDiceEntity

    @Serializable
    @SerialName(value = REROLL)
    data class Reroll(val originalFace: Int, val newFace: Int) : RolledDiceEntity

    companion object {
        private const val FACES = "Faces"
        private const val REROLL = "Reroll"
    }
}

@Serializable
data class LatestRollEntity(
    val result: RolledDiceEntity? = null,
    val messageId: String? = null,
    val latestMessageId: String? = null,
)

internal fun LatestRollEntity.toDomain(): LatestRoll =
    LatestRoll(
        result = result?.toDomain(),
        messageId = messageId,
        latestMessageId = latestMessageId,
    )

private fun RolledDiceEntity.toDomain(): RolledDice =
    when (this) {
        is RolledDiceEntity.Faces -> {
            val slotFaces =
                if (secondDie == null) {
                    mapOf(FIRST_DIE_SLOT to firstDie)
                } else {
                    mapOf(FIRST_DIE_SLOT to firstDie, SECOND_DIE_SLOT to secondDie)
                }
            RolledDice.Faces(slotFaces = slotFaces)
        }

        is RolledDiceEntity.Reroll ->
            RolledDice.Reroll(originalFace = originalFace, newFace = newFace)
    }

internal fun RolledDice.toEntity(): RolledDiceEntity =
    when (this) {
        is RolledDice.Faces ->
            RolledDiceEntity.Faces(
                firstDie = slotFaces.getValue(key = FIRST_DIE_SLOT),
                secondDie = slotFaces[SECOND_DIE_SLOT],
            )

        is RolledDice.Reroll ->
            RolledDiceEntity.Reroll(originalFace = originalFace, newFace = newFace)
    }
