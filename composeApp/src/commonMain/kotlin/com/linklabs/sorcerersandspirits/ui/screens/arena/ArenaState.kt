package com.linklabs.sorcerersandspirits.ui.screens.arena

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.arena.ArenaPhase
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

@Immutable
data class ArenaState(
    val arenaId: String,
    val phase: ArenaPhase,
    val playerStates: List<PlayerState>,
    private val activePlayerIndex: Int,
) {
    val activePlayer = playerStates[activePlayerIndex]
}

@Immutable
data class PlayerState(
    val userId: String,
    val displayName: String,
    val characterClassState: CharacterClassState?,
)

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
