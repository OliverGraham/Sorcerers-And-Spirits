package com.linklabs.sorcerersandspirits.ui.screens.gamerules

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.forAbility

@Immutable
data class GameRulesState(
    val combatRules: List<String>,
    val gameTerms: List<String>,
    val classes: List<ClassRulesState>,
)

@Immutable
data class ClassRulesState(
    val className: String,
    val hitPoints: Int,
    val armorClass: Int,
    val speed: Int,
    val offensiveAbility: AbilityRulesState,
    val defensiveAbility: AbilityRulesState,
)

@Immutable data class AbilityRulesState(val name: String, val description: String)

internal fun CharacterClassState.toRulesState(copy: CharacterClassCopy): ClassRulesState {
    val offensiveCopy = copy.forAbility(ability = offensiveAbility)
    val defensiveCopy = copy.forAbility(ability = defensiveAbility)

    return ClassRulesState(
        className = className,
        hitPoints = hitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility =
        AbilityRulesState(name = offensiveCopy.name, description = offensiveCopy.long),
        defensiveAbility =
        AbilityRulesState(name = defensiveCopy.name, description = defensiveCopy.long),
    )
}
