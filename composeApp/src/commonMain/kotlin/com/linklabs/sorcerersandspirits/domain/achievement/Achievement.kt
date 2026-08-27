package com.linklabs.sorcerersandspirits.domain.achievement

import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats

// TODO: Delete this docstring and add steps ina real document of some kind

/**
 * One unlockable achievement: its [id] and the rule that unlocks it, both owned by the app. Copy
 * (name, description) is a presentation concern resolved from string resources in the UI layer,
 * keyed by [id], so the domain stays free of presentation.
 *
 * To add an achievement derivable from recorded stats: add an [AchievementId], add a data object
 * here with its predicate, list it in [AchievementCatalog], and add its string resources. Rules
 * that need in-match event state beyond the persisted counters are SS-127.
 */
sealed interface Achievement {
    val id: AchievementId

    fun isUnlockedBy(stats: ProfileStats): Boolean
}

data object WinAMatch : Achievement {
    override val id = AchievementId.WinAMatch

    override fun isUnlockedBy(stats: ProfileStats): Boolean = stats.overall.matchesWon >= 1
}

data object WinAGauntlet : Achievement {
    override val id = AchievementId.WinAGauntlet

    override fun isUnlockedBy(stats: ProfileStats): Boolean = stats.overall.gauntletsWon >= 1
}

data object WinAMatchWithEveryClass : Achievement {
    override val id = AchievementId.WinAMatchWithEveryClass

    override fun isUnlockedBy(stats: ProfileStats): Boolean = CharacterClass.playableTypes().all { classType ->
        stats.hasMatchWinAs(classType = classType)
    }
}

data object WinAGauntletWithEveryClass : Achievement {
    override val id = AchievementId.WinAGauntletWithEveryClass

    override fun isUnlockedBy(stats: ProfileStats): Boolean = CharacterClass.playableTypes().all { classType ->
        stats.hasGauntletWinAs(classType = classType)
    }
}
