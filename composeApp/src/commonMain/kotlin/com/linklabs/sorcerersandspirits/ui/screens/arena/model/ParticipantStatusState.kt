package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.model.ParticipantStatus

@Immutable
sealed interface ParticipantStatusState {

    @Immutable data object Conscious : ParticipantStatusState

    @Immutable data class KnockedOut(val knockoutOrder: Int) : ParticipantStatusState
}

internal fun ParticipantStatus.toState(): ParticipantStatusState = when (this) {
    is ParticipantStatus.Conscious -> ParticipantStatusState.Conscious
    is ParticipantStatus.KnockedOut -> ParticipantStatusState.KnockedOut(knockoutOrder = knockoutOrder)
}

internal fun ParticipantStatusState.toDomain(): ParticipantStatus = when (this) {
    is ParticipantStatusState.Conscious -> ParticipantStatus.Conscious
    is ParticipantStatusState.KnockedOut -> ParticipantStatus.KnockedOut(knockoutOrder = knockoutOrder)
}
