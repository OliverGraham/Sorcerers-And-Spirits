package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutations
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

class DodgeOutcome(
    private val combatMutations: CombatMutations,
    private val dodge: DefensiveAbility.Dodge,
) {
    fun applyOutcome() {
        combatMutations.applyOutcome()
    }

    private fun CombatMutations.applyOutcome() {
        val didArmorIncrease = dodge.armorBonus < 2

        if (didArmorIncrease) {
            // TODO in UI layer: Add ability and AC together to get end result

            val updatedDodge = dodge.copy(armorBonus = dodge.armorBonus + 1)
            updateDefenderClass { playerClass -> playerClass.copy(defensiveAbility = updatedDodge) }
        }

        addMessage(message = OutgoingMessage.Dodge(didArmorIncrease = didArmorIncrease))
    }
}
