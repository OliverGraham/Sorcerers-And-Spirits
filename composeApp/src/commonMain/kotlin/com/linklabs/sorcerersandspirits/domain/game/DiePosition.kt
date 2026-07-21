package com.linklabs.sorcerersandspirits.domain.game

enum class DiePosition {
    First,
    Second;

    companion object {
        fun getRandomPosition() = DiePosition.entries.random()
    }
}

enum class MirrorImageRerollSelection {
    First,
    Second,
    None,
}
