package com.linklabs.sorcerersandspirits.ui.common.achievement

import com.linklabs.sorcerersandspirits.domain.achievement.AchievementId
import org.jetbrains.compose.resources.StringResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.achievement_win_a_gauntlet_description
import sorcerersandspirits.composeapp.generated.resources.achievement_win_a_gauntlet_every_class_description
import sorcerersandspirits.composeapp.generated.resources.achievement_win_a_gauntlet_every_class_name
import sorcerersandspirits.composeapp.generated.resources.achievement_win_a_gauntlet_name
import sorcerersandspirits.composeapp.generated.resources.achievement_win_a_match_description
import sorcerersandspirits.composeapp.generated.resources.achievement_win_a_match_every_class_description
import sorcerersandspirits.composeapp.generated.resources.achievement_win_a_match_every_class_name
import sorcerersandspirits.composeapp.generated.resources.achievement_win_a_match_name

val AchievementId.displayNameResource: StringResource
    get() =
        when (this) {
            AchievementId.WinAMatch -> Res.string.achievement_win_a_match_name

            AchievementId.WinAGauntlet -> Res.string.achievement_win_a_gauntlet_name

            AchievementId.WinAMatchWithEveryClass ->
                Res.string.achievement_win_a_match_every_class_name

            AchievementId.WinAGauntletWithEveryClass ->
                Res.string.achievement_win_a_gauntlet_every_class_name
        }

val AchievementId.descriptionResource: StringResource
    get() =
        when (this) {
            AchievementId.WinAMatch -> Res.string.achievement_win_a_match_description

            AchievementId.WinAGauntlet -> Res.string.achievement_win_a_gauntlet_description

            AchievementId.WinAMatchWithEveryClass ->
                Res.string.achievement_win_a_match_every_class_description

            AchievementId.WinAGauntletWithEveryClass ->
                Res.string.achievement_win_a_gauntlet_every_class_description
        }
