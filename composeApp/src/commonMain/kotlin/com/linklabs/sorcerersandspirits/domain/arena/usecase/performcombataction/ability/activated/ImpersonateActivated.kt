package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.defaultState

internal class ImpersonateActivated(
    private val combatMutator: CombatMutator,
    private val impersonateAbility: OffensiveAbility.Impersonate,
) {
    operator fun invoke() = combatMutator.impersonateOutcome()

    private fun CombatMutator.impersonateOutcome() {
        val copiedBehavior =
            impersonateAbility.copy(
                impersonatedAbility = impersonateAbility.impersonatedAbility.defaultState
            )

        addMessage(
            message =
                ArenaMessage.Impersonate(
                    illusionistUserId = attackerUserId,
                    impersonateAbility = copiedBehavior,
                )
        )

        updateAttackerClass { attacker -> attacker.copy(offensiveAbility = copiedBehavior) }

        setNextCombatAction(
            nextAction = NextCombatAction.Idle(activeUserIds = setOf(attackerUserId))
        )
    }
}
