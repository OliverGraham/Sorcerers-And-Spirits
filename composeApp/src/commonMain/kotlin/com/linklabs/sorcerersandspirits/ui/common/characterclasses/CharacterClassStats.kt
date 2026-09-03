package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.ui.unit.dp
import com.linklabs.sorcerersandspirits.domain.game.AbilityCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.ClassCopyKeys
import org.jetbrains.compose.resources.DrawableResource
import sorcerersandspirits.composeapp.generated.resources.Res
import sorcerersandspirits.composeapp.generated.resources.ic_armor_class
import sorcerersandspirits.composeapp.generated.resources.ic_defensive_ability
import sorcerersandspirits.composeapp.generated.resources.ic_hit_points
import sorcerersandspirits.composeapp.generated.resources.ic_offensive_ability
import sorcerersandspirits.composeapp.generated.resources.ic_speed

internal val STAT_ICON_SIZE = 28.dp

internal data class AvatarStat(
    val icon: DrawableResource,
    val value: String?,
    val name: String,
    val explanation: String,
)

internal fun avatarStats(
    characterClass: CharacterClassState,
    classCopy: CharacterClassCopy,
    isDetailed: Boolean = false,
): List<AvatarStat> {
    val speed = classCopy[ClassCopyKeys.SPEED]
    val armor = classCopy[ClassCopyKeys.ARMOR]
    val health = classCopy[ClassCopyKeys.HEALTH]
    val offensive = classCopy.forAbility(ability = characterClass.offensiveAbility)
    val defensive = classCopy.forAbility(ability = characterClass.defensiveAbility)

    fun AbilityCopy.explanation(): String = if (isDetailed) long else short

    val hasOffensive = characterClass.offensiveAbility !is OffensiveAbilityState.None
    val hasDefensive = characterClass.defensiveAbility !is DefensiveAbilityState.None

    return buildList {
        add(
            AvatarStat(
                icon = Res.drawable.ic_speed,
                value = characterClass.speed.toString(),
                name = speed.name,
                explanation = speed.explanation(),
            ),
        )
        add(
            AvatarStat(
                icon = Res.drawable.ic_armor_class,
                value = characterClass.armorClassDisplay,
                name = armor.name,
                explanation = armor.explanation(),
            ),
        )
        add(
            AvatarStat(
                icon = Res.drawable.ic_hit_points,
                value = characterClass.hitPointsDisplay,
                name = health.name,
                explanation = health.explanation(),
            ),
        )
        if (hasOffensive) {
            add(
                AvatarStat(
                    icon = Res.drawable.ic_offensive_ability,
                    value = null,
                    name = offensive.name,
                    explanation = offensive.explanation(),
                ),
            )
        }
        if (hasDefensive) {
            add(
                AvatarStat(
                    icon = Res.drawable.ic_defensive_ability,
                    value = null,
                    name = defensive.name,
                    explanation = defensive.explanation(),
                ),
            )
        }
    }
}
