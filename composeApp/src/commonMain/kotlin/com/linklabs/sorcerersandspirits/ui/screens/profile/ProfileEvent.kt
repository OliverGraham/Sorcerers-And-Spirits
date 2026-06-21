package com.linklabs.sorcerersandspirits.ui.screens.profile

import com.linklabs.sorcerersandspirits.domain.profile.DisplayNameResult

sealed interface ProfileEvent {
    /**
     * The profile was deleted and a fresh anonymous one was provisioned in its place; the screen
     * pops back so the player returns to the app on the new profile.
     */
    data object ProfileDeleted : ProfileEvent

    /**
     * A display-name save finished with [result]; the screen surfaces the outcome as a snackbar.
     * Delivered as a one-shot event so it shows once rather than re-firing on recomposition.
     */
    data class DisplayNameUpdated(val result: DisplayNameResult) : ProfileEvent
}
