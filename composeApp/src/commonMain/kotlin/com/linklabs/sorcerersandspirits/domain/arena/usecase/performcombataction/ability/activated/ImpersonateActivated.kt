package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class ImpersonateActivated(
    private val combatMutator: CombatMutator,
    private val impersonateAbility: OffensiveAbility.Impersonate,
) {
    operator fun invoke() = combatMutator.impersonateOutcome()

    private fun CombatMutator.impersonateOutcome(): CombatActionUpdates {
        addMessage(
            message =
                OutgoingMessage.Impersonate(
                    illusionistUserId = attackerUserId,
                    impersonateAbility = impersonateAbility,
                )
        )

        updateAttackerClass { attacker -> attacker.copy(offensiveAbility = impersonateAbility) }

        return endPhaseOnSameUserTurn(userId = attackerUserId)
    }
}
