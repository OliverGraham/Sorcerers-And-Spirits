package com.linklabs.sorcerersandspirits.domain.achievement

/**
 * The single registry of every achievement the app can award. Adding an [Achievement] data object
 * here is all that's needed for it to be evaluated and unlocked; its player-facing copy is resolved
 * from string resources keyed by [AchievementId] in the UI layer.
 */
object AchievementCatalog {
    val all: List<Achievement> =
        listOf(WinAMatch, WinAGauntlet, WinAMatchWithEveryClass, WinAGauntletWithEveryClass)
}
