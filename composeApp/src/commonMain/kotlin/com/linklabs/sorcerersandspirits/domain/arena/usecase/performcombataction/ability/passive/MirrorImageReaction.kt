package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityRollPurpose
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.CombatActionUpdates
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations

class MirrorImageReaction(
    private val combatMutations: CombatMutations,
    private val firstDie: Int,
    private val secondDie: Int,
    private val triggeredByArmorRoll: Boolean,
) {
    operator fun invoke(): CombatActionUpdates {
        // Wait for user choice: Reroll or Pass.
        // Returning current state to show options in UI.
        return CombatActionUpdates(
            nextActivePlayerUserId = combatMutations.defenderUserId,
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
