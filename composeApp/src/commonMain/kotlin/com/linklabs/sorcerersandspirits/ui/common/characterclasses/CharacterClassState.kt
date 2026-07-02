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

    /**
     * The armor class shown to the player: base alone, or base plus the active Rogue Dodge bonus
     * (e.g. "13 + 1") so the earned bonus stays visibly distinct, mirroring [hitPointsDisplay]. The
     * domain folds the bonus into [armorClass], so the base is recovered by removing the bonus.
     */
    val armorClassDisplay: String = buildArmorClassDisplay()

    private fun buildHitPointsDisplay(): String {
        val shieldPoints =
            (offensiveAbility.effective as? OffensiveAbilityState.SummonMinion)?.shieldPoints ?: 0
        return if (shieldPoints > 0) "$hitPoints + $shieldPoints" else hitPoints.toString()
    }

    private fun buildArmorClassDisplay(): String {
        val dodgeBonus = (defensiveAbility as? DefensiveAbilityState.Dodge)?.armorBonus ?: 0
        return if (dodgeBonus > 0) {
            "${armorClass - dodgeBonus} + $dodgeBonus"
        } else {
            armorClass.toString()
        }
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
