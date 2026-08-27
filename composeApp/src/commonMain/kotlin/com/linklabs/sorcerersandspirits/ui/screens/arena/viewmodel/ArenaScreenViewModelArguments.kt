package com.linklabs.sorcerersandspirits.ui.screens.arena.viewmodel

import com.linklabs.sorcerersandspirits.domain.computer.ComputerOpponent

data class ArenaScreenViewModelArguments(
    val arenaId: String,
    val expectedNumberOfPlayers: Int,
    val computerOpponents: List<ComputerOpponent> = emptyList(),
)
