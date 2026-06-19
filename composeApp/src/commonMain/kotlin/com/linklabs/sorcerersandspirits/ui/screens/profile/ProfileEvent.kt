package com.linklabs.sorcerersandspirits.ui.screens.profile

sealed interface ProfileEvent {
    /**
     * The profile was deleted and a fresh anonymous one was provisioned in its place; the screen
     * pops back so the player returns to the app on the new profile.
     */
    data object ProfileDeleted : ProfileEvent
}
