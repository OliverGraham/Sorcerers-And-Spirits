package com.linklabs.sorcerersandspirits.ui.screens.arena.model

// TODO Arena: Is this the right idea?

sealed interface ArenaUpdate {
    data object PlayersJoining : ArenaUpdate

    data class PlayersChoosingClasses(val activePlayerId: String) : ArenaUpdate

    data class Combat(val step: CombatStep) : ArenaUpdate

    data object Finish : ArenaUpdate
}

sealed interface CombatStep {
    data class PlayerChoosingTarget(val actingPlayerId: String, val targetIds: Set<String>) :
        CombatStep

    data class PlayerRollingAgainstAc(val actingPlayerId: String, val targetIds: Set<String>) :
        CombatStep

    data class PlayerRollingAgainstHp(val actingPlayerId: String, val targetIds: Set<String>) :
        CombatStep
}
