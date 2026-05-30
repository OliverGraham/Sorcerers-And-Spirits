package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.OutgoingArenaMessage
import com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.combatmutations.CombatMutator
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility

internal class TriggerDodge(
    private val combatMutator: CombatMutator,
    private val dodge: DefensiveAbility.Dodge,
) {
    operator fun invoke() {
        combatMutator.triggerDodge()
    }

    private fun CombatMutator.triggerDodge() {
        val didArmorIncrease = dodge.armorBonus < 2

        if (didArmorIncrease) {
            // TODO in UI layer: Add ability and AC together to get end result

            val updatedDodge = dodge.copy(armorBonus = dodge.armorBonus + 1)
            updateDefenderClass { playerClass -> playerClass.copy(defensiveAbility = updatedDodge) }
        }

        addMessage(message = OutgoingArenaMessage.Dodge(didArmorIncrease = didArmorIncrease))
    }
}
