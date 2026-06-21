package com.linklabs.sorcerersandspirits.ui.screens.gamerules.ui.preview

import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassStates
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.GameRulesState
import com.linklabs.sorcerersandspirits.ui.screens.gamerules.toRulesState

/** Reusable sample state for GameRules composable previews. */
val exampleGameRulesState =
    GameRulesState(
        combatRules =
            listOf(
                "An Arena match has 2 to 8 players, each playing a unique class.",
                "Attack with 2d6 — first roll versus Armor Class, then versus Health.",
            ),
        gameTerms =
            listOf(
                "Turn — one player attacking or using an ability.",
                "Gauntlet — a run of 3 consecutive matches in the same Arena.",
            ),
        classes =
            previewCharacterClassStates().map {
                it.toRulesState(copy = previewCharacterClassCopy())
            },
    )
