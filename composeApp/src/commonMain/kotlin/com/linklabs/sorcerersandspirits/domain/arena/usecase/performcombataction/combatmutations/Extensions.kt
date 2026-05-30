package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations

import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive.holyshield.TriggerHolyShield
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

internal fun CombatMutator.triggerHolyShieldIfApplicable(
    defenderDefensiveAbility: DefensiveAbility
): Boolean {
    val shouldTriggerHolyShield =
        defenderDefensiveAbility is DefensiveAbility.HolyShield &&
            !defenderDefensiveAbility.usedThisMatch

    if (shouldTriggerHolyShield) {
        TriggerHolyShield(combatMutator = this, holyShield = defenderDefensiveAbility)
    }

    return shouldTriggerHolyShield
}
