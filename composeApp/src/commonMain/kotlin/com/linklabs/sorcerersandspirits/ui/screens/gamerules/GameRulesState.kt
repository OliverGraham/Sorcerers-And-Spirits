package com.linklabs.sorcerersandspirits.ui.screens.gamerules

import androidx.compose.runtime.Immutable
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.defensiveAbilityDescription
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.defensiveAbilityName
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.offensiveAbilityDescription
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.offensiveAbilityName

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

internal fun CharacterClassState.toRulesState() =
    ClassRulesState(
        className = className,
        hitPoints = hitPoints,
        armorClass = armorClass,
        speed = speed,
        offensiveAbility =
            AbilityRulesState(
                name = offensiveAbilityName(ability = offensiveAbility),
                description = offensiveAbilityDescription(ability = offensiveAbility),
            ),
        defensiveAbility =
            AbilityRulesState(
                name = defensiveAbilityName(ability = defensiveAbility),
                description = defensiveAbilityDescription(ability = defensiveAbility),
            ),
    )
