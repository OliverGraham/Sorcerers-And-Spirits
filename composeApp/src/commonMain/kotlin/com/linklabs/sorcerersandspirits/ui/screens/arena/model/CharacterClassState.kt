package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

@Immutable
sealed interface CharacterClassState {
    val hitPoints: Int
    val armorClass: Int
    val speed: Int
    val offensiveAbility: OffensiveAbility
    val defensiveAbility: DefensiveAbility
    val className: String

    @Immutable
    data class Rogue(
        override val hitPoints: Int,
        override val armorClass: Int,
        override val speed: Int,
        override val offensiveAbility: OffensiveAbility,
        override val defensiveAbility: DefensiveAbility,
        override val className: String,
    ) : CharacterClassState

    @Immutable
    data class Mage(
        override val hitPoints: Int,
        override val armorClass: Int,
        override val speed: Int,
        override val offensiveAbility: OffensiveAbility,
        override val defensiveAbility: DefensiveAbility,
        override val className: String,
    ) : CharacterClassState

    @Immutable
    data class Paladin(
        override val hitPoints: Int,
        override val armorClass: Int,
        override val speed: Int,
        override val offensiveAbility: OffensiveAbility,
        override val defensiveAbility: DefensiveAbility,
        override val className: String,
    ) : CharacterClassState
}

internal fun CharacterClass.toState() =
    when (this) {
        is CharacterClass.Rogue ->
            CharacterClassState.Rogue(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
        is CharacterClass.Mage ->
            CharacterClassState.Mage(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
        is CharacterClass.Paladin ->
            CharacterClassState.Paladin(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
    }

internal fun CharacterClassState.toDomain() =
    when (this) {
        is CharacterClassState.Rogue ->
            CharacterClass.Rogue(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
        is CharacterClassState.Mage ->
            CharacterClass.Mage(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
        is CharacterClassState.Paladin ->
            CharacterClass.Paladin(
                hitPoints = hitPoints,
                armorClass = armorClass,
                speed = speed,
                offensiveAbility = offensiveAbility,
                defensiveAbility = defensiveAbility,
                className = className,
            )
    }
