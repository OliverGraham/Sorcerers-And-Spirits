package com.linklabs.sorcerersandspirits.domain.audio

sealed interface SoundEffect {
    val assetFileName: String

    data object DiceSettle : SoundEffect {
        override val assetFileName: String = "dice_settle.wav"
    }

    data object ArmorBreak : SoundEffect {
        override val assetFileName: String = "armor_break.wav"
    }

    data object ArmorHold : SoundEffect {
        override val assetFileName: String = "armor_hold.wav"
    }

    data object Damage : SoundEffect {
        override val assetFileName: String = "damage.wav"
    }

    data object Knockout : SoundEffect {
        override val assetFileName: String = "knockout.wav"
    }
}
