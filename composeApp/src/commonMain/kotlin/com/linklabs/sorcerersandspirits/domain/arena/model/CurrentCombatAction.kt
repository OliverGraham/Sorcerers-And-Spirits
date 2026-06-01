package com.linklabs.sorcerersandspirits.domain.arena.model

/** Action the user is taking (UI -> Domain (then is transformed into [NextCombatAction]) */
sealed interface CurrentCombatAction {
    data class NormalAttack(val roll: NormalAttackRoll) : CurrentCombatAction

    data class AbilityRoll(val purpose: AbilityAction) : CurrentCombatAction
}
