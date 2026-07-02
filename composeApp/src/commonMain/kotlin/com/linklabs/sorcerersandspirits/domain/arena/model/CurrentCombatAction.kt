package com.linklabs.sorcerersandspirits.domain.arena.model

/** Action the user is taking (UI -> Domain (then is transformed into [NextCombatAction]) */
sealed interface CurrentCombatAction {
    val attackerUserId: String
    val defenderUserId: String
    val originalTurnUserId: String

    data class NormalAttack(
        val roll: NormalAttackType,
        override val attackerUserId: String,
        override val defenderUserId: String,
        override val originalTurnUserId: String,
    ) : CurrentCombatAction

    /**
     * [rollingUserId] is the combatant who dispatched this roll. In a simultaneous roll-off (Mage
     * Teleport) both combatants hold the roll button at once, so the roller decides which of the
     * two dice is rolled — matched against each [AbilityAction.Passive.Teleport.TeleportDie.userId]
     * — otherwise whoever tapped first would roll the other combatant's die.
     */
    data class AbilityRoll(
        val purpose: AbilityAction,
        override val attackerUserId: String,
        override val defenderUserId: String,
        override val originalTurnUserId: String,
        val rollingUserId: String,
    ) : CurrentCombatAction
}
