package com.linklabs.sorcerersandspirits.domain.arena.model

sealed interface ArenaMessage {
    val id: String

    data class Round(override val id: String, val roundNumber: Int) : ArenaMessage

    data class Action(
        override val id: String,
        val rollerName: String,
        val rollerValue: Int,
        val defenderName: String,
        val rollType: RollType,
    ) : ArenaMessage

    data class Result(
        override val id: String,
        val rollerName: String,
        val defenderName: String,
        val rollType: RollType,
    ) : ArenaMessage

    data class Generic(override val id: String, val message: String) : ArenaMessage
}

enum class RollType {
    Ability,
    ArmorClass,
    Health,
}
