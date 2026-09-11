package com.linklabs.sorcerersandspirits.domain.game

enum class MinionType {
    Skeleton,
    Golem,
}

interface MinionAttributes {
    val type: MinionType
    val name: String
    val hasAttackedThisRound: Boolean
    val hitPoints: Int
}

data class Minion(
    override val type: MinionType,
    override val name: String,
    override val hasAttackedThisRound: Boolean,
    override val hitPoints: Int,
) : MinionAttributes
