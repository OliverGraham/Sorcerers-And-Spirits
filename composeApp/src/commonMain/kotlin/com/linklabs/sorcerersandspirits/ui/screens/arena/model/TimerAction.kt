package com.linklabs.sorcerersandspirits.ui.screens.arena.model

sealed interface TimerAction {
    data class Start(val timerDuration: Int) : TimerAction

    data object None : TimerAction

    data object Stop : TimerAction
}
