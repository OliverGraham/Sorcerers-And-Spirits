package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface ParticipantStatus {

    data object Conscious : ParticipantStatus

    data class KnockedOut(val knockoutOrder: Int) : ParticipantStatus

    data object Left : ParticipantStatus
}
