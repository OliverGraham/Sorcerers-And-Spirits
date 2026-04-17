package com.linklabs.sorcerersandspirits.ui.screens.arena

// TODO Arena: figure out the logs
//   Text log can contain 3 lines in preview, and the rest below
//          RoundMessage  // "Round 1"
//          ActionMessage // "Rogue rolled 6 against Paladin's AC"
//          ResultMessage // "Rogue is now rolling against Paladin's health!"

// CHatGPT idea
sealed interface ArenaMessageLogEntry {
    val id: String
    val timestampMs: Long

    data class PlayerJoined(
        override val id: String,
        override val timestampMs: Long,
        val playerName: String,
    ) : ArenaMessageLogEntry

    data class ClassChosen(
        override val id: String,
        override val timestampMs: Long,
        val playerName: String,
        val className: String,
    ) : ArenaMessageLogEntry

    data class AttackRolled(
        override val id: String,
        override val timestampMs: Long,
        val attackerName: String,
        val defenderName: String,
        val roll: Int,
        val targetArmorClass: Int,
    ) : ArenaMessageLogEntry

    data class DamageApplied(
        override val id: String,
        override val timestampMs: Long,
        val sourceName: String,
        val targetName: String,
        val damage: Int,
        val remainingHp: Int,
    ) : ArenaMessageLogEntry
}
