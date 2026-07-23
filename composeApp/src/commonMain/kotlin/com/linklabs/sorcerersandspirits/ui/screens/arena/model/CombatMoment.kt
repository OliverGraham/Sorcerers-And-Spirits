package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable

@Immutable
data class CombatMoment(
    val seating: CombatSeating,
    val activeUserIds: Set<String>,
    val turnOwnerUserId: String,
)

@Immutable
data class CombatMomentHold(
    val displayedMoment: CombatMoment,
    val heldMoment: CombatMoment,
    val settledAnimationMessageId: String?,
    val wasSequencing: Boolean,
) {
    companion object {
        fun initial(liveMoment: CombatMoment, latestAnimationMessageId: String?): CombatMomentHold =
            CombatMomentHold(
                displayedMoment = liveMoment,
                heldMoment = liveMoment,
                settledAnimationMessageId = latestAnimationMessageId,
                wasSequencing = false,
            )
    }
}

fun CombatMomentHold.advancedWith(
    liveMoment: CombatMoment,
    latestAnimationMessageId: String?,
    isSequencing: Boolean,
): CombatMomentHold {
    val isSequencerFallingEdge = wasSequencing && !isSequencing
    val isResolving = isSequencing || latestAnimationMessageId != settledAnimationMessageId
    val isSettling = isSequencerFallingEdge || !isResolving

    return CombatMomentHold(
        displayedMoment = if (isSettling) liveMoment else heldMoment,
        heldMoment = if (isSettling) liveMoment else heldMoment,
        settledAnimationMessageId =
            if (isSettling) latestAnimationMessageId else settledAnimationMessageId,
        wasSequencing = isSequencing,
    )
}
