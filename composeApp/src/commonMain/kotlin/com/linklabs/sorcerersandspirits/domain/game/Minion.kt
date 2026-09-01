package com.linklabs.sorcerersandspirits.domain.game

enum class MinionType {
    Skeleton,
    Golem,
}

interface MinionAttributes {
    val type: MinionType
    val hasAttackedThisRound: Boolean
}

data class Minion(
    override val type: MinionType,
    override val hasAttackedThisRound: Boolean,
) : MinionAttributes
