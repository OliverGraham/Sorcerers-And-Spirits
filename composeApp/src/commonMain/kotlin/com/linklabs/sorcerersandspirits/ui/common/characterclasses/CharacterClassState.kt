package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.shieldPoints

@Immutable
data class CharacterClassState(
    val type: CharacterClassType,
    val hitPoints: Int,
    val maxHitPoints: Int,
    val armorClass: Int,
    val speed: Int,
    val offensiveAbility: OffensiveAbilityState,
    val defensiveAbility: DefensiveAbilityState,
) {
    val className: String = type.name
    val isConscious: Boolean = hitPoints > 0

    /**
     * The hit points shown to the player: base alone, or base plus the minion shield contribution
     * (e.g. "7 + 2") so base and shield health stay visibly distinct.
     */
    val hitPointsDisplay: String = buildHitPointsDisplay()

    private fun buildHitPointsDisplay(): String {
        val shieldPoints =
            (offensiveAbility as? OffensiveAbilityState.SummonMinion)?.shieldPoints ?: 0
        return if (shieldPoints > 0) "$hitPoints + $shieldPoints" else hitPoints.toString()
    }
}

internal fun CharacterClass.toState() =
    CharacterClassState(
        type = type,
        hitPoints = hitPoints,
        maxHitPoints = maxHitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility = offensiveAbility.toState(),
        defensiveAbility = defensiveAbility.toState(),
    )

internal fun CharacterClassState.toDomain() =
    CharacterClass(
        type = type,
        hitPoints = hitPoints,
        maxHitPoints = maxHitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility = offensiveAbility.toDomain(),
        defensiveAbility = defensiveAbility.toDomain(),
    )
