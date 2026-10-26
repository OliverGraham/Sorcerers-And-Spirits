package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.ParticipantStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ParticipantStatusEntity {

    @Serializable
    @SerialName(value = CONSCIOUS)
    data object Conscious : ParticipantStatusEntity

    @Serializable
    @SerialName(value = KNOCKED_OUT)
    data class KnockedOut(val knockoutOrder: Int) : ParticipantStatusEntity

    @Serializable
    @SerialName(value = LEFT)
    data object Left : ParticipantStatusEntity

    companion object {
        private const val CONSCIOUS = "Conscious"
        private const val KNOCKED_OUT = "KnockedOut"
        private const val LEFT = "Left"
    }
}

internal fun ParticipantStatusEntity.toDomain(): ParticipantStatus = when (this) {
    is ParticipantStatusEntity.Conscious -> ParticipantStatus.Conscious
    is ParticipantStatusEntity.KnockedOut -> ParticipantStatus.KnockedOut(knockoutOrder = knockoutOrder)
    is ParticipantStatusEntity.Left -> ParticipantStatus.Left
}

internal fun ParticipantStatus.toEntity(): ParticipantStatusEntity = when (this) {
    is ParticipantStatus.Conscious -> ParticipantStatusEntity.Conscious
    is ParticipantStatus.KnockedOut -> ParticipantStatusEntity.KnockedOut(knockoutOrder = knockoutOrder)
    is ParticipantStatus.Left -> ParticipantStatusEntity.Left
}
