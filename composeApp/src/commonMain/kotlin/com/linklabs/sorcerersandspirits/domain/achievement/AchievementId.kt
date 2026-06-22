package com.linklabs.sorcerersandspirits.domain.achievement

/**
 * The stable, type-safe identity of every achievement and the single source of truth for it.
 * Persisted unlock state is matched back to a value here by name, and an unrecognized stored key is
 * ignored rather than breaking.
 */
enum class AchievementId {
    WinAMatch,
    WinAGauntlet,
    WinAMatchWithEveryClass,
    WinAGauntletWithEveryClass,
}
