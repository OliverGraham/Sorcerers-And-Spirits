package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations

import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction

data class CombatActionUpdates(val nextActivePlayerUserId: String, val nextStep: NextCombatAction)
