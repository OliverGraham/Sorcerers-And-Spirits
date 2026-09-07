package com.linklabs.sorcerersandspirits.domain.game

enum class MinionType {
    Skeleton,
    Golem,
}

interface MinionAttributes {
    val type: MinionType
    val name: String
    val hasAttackedThisRound: Boolean
}

data class Minion(
    override val type: MinionType,
    override val name: String,
    override val hasAttackedThisRound: Boolean,
) : MinionAttributes
