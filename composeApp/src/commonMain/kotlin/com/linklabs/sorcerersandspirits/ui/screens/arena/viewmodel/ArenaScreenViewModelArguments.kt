package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel

data class ArenaScreenViewModelArguments(
    val arenaId: String,
    val expectedNumberOfPlayers: Int,
    val computerOpponentUserId: String? = null,
)
