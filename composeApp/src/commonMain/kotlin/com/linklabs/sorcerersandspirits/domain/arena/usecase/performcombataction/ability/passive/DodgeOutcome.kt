package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

internal class DodgeOutcome(
    private val combatMutator: CombatMutator,
    private val dodge: DefensiveAbility.Dodge,
) {
    fun applyOutcome() {
        combatMutator.applyOutcome()
    }

    private fun CombatMutator.applyOutcome() {
        val didArmorIncrease = dodge.armorBonus < 2

        if (didArmorIncrease) {
            // TODO in UI layer: Add ability and AC together to get end result

            val updatedDodge = dodge.copy(armorBonus = dodge.armorBonus + 1)
            updateDefenderClass { playerClass -> playerClass.copy(defensiveAbility = updatedDodge) }
        }

        addMessage(message = OutgoingMessage.Dodge(didArmorIncrease = didArmorIncrease))
    }
}
