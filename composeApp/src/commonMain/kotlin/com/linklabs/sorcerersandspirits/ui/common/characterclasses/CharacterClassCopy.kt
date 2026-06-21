package com.linklabs.sorcerersandspirits.ui.common.characterclasses

import androidx.compose.runtime.staticCompositionLocalOf
import com.linklabs.sorcerersandspirits.domain.game.AbilityCopy
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassCopy
import com.linklabs.sorcerersandspirits.domain.game.ClassCopyKeys

/**
 * The active class stat/ability copy for the current screen (SS-081). Provided from each screen's
 * state; defaults to [CharacterClassCopy.EMPTY] so a screen that forgets to provide it renders
 * blank copy rather than crashing.
 */
val LocalCharacterClassCopy = staticCompositionLocalOf { CharacterClassCopy.EMPTY }

internal fun OffensiveAbilityState.copyKey(): String? =
    when (this) {
        is OffensiveAbilityState.KillingMomentum -> ClassCopyKeys.KILLING_MOMENTUM
        is OffensiveAbilityState.AllIn -> ClassCopyKeys.ALL_IN
        is OffensiveAbilityState.PrepareAttack -> ClassCopyKeys.PREPARE_ATTACK
        is OffensiveAbilityState.SummonMinion -> ClassCopyKeys.SUMMON_MINION
        is OffensiveAbilityState.Encore -> ClassCopyKeys.ENCORE
        is OffensiveAbilityState.Impersonate -> ClassCopyKeys.IMPERSONATE
        is OffensiveAbilityState.SpellPotency -> ClassCopyKeys.SPELL_POTENCY
        is OffensiveAbilityState.None -> null
    }

internal fun DefensiveAbilityState.copyKey(): String? =
    when (this) {
        is DefensiveAbilityState.Dodge -> ClassCopyKeys.DODGE
        is DefensiveAbilityState.LuckyCoin -> ClassCopyKeys.LUCKY_COIN
        is DefensiveAbilityState.Riposte -> ClassCopyKeys.RIPOSTE
        is DefensiveAbilityState.Persuade -> ClassCopyKeys.PERSUADE
        is DefensiveAbilityState.MirrorImage -> ClassCopyKeys.MIRROR_IMAGE
        is DefensiveAbilityState.Teleport -> ClassCopyKeys.TELEPORT
        is DefensiveAbilityState.HolyShield -> ClassCopyKeys.HOLY_SHIELD
        is DefensiveAbilityState.None -> null
    }

internal fun CharacterClassCopy.forAbility(ability: OffensiveAbilityState): AbilityCopy {
    val key = ability.copyKey() ?: return AbilityCopy.EMPTY
    return this[key]
}

internal fun CharacterClassCopy.forAbility(ability: DefensiveAbilityState): AbilityCopy {
    val key = ability.copyKey() ?: return AbilityCopy.EMPTY
    return this[key]
}
