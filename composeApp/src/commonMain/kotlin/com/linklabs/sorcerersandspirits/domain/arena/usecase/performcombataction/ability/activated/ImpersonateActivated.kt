package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.activated

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.game.OffensiveAbility

internal class ImpersonateActivated(
    private val combatMutations: CombatMutations,
    private val impersonateAbility: OffensiveAbility.Impersonate,
) {
    operator fun invoke() = combatMutations.impersonateOutcome()

    private fun CombatMutations.impersonateOutcome(): CombatActionUpdates {
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
