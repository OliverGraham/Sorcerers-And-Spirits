package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator

internal class MirrorImageReaction(
    private val combatMutator: CombatMutator,
    private val firstDie: Int,
    private val secondDie: Int,
    private val triggeredByArmorRoll: Boolean,
) {
    operator fun invoke(): CombatActionUpdates {
        // Wait for user choice: Reroll or Pass.
        // Returning current state to show options in UI.
        return CombatActionUpdates(
            nextActivePlayerUserId = combatMutator.defenderUserId,
            nextStep =
                NextCombatAction.AbilityRoll(
                    purpose =
                        AbilityRollPurpose.Passive.Reaction.MirrorImage(
                            firstDie = firstDie,
                            secondDie = secondDie,
                            triggeredByArmorRoll = triggeredByArmorRoll,
                        )
                ),
        )
    }
}
