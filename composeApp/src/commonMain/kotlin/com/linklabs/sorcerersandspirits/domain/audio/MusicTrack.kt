package com.linklabs.sorcerersandspirits.domain.audio

sealed interface MusicTrack {
    val assetFileName: String

    data object TavernTheme : MusicTrack {
        override val assetFileName: String = "tavern_theme.m4a"
    }

    data object ArenaBattle : MusicTrack {
        override val assetFileName: String = "arena_battle.m4a"
    }
}
