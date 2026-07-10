package com.linklabs.sorcerersandspirits.domain.arena.usecase.performcombataction.ability.passive

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMessage
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
        val didArmorIncrease = dodge.armorBonus < abilityConfig.dodgeMaxArmorBonus

        if (didArmorIncrease) {
            val updatedDodge = dodge.copy(armorBonus = dodge.armorBonus + 1)
            updateDefenderClass { playerClass ->
                playerClass.copy(
                    armorClass = playerClass.armorClass + 1,
                    defensiveAbility = updatedDodge,
                )
            }
        }

        addMessage(
            message =
                ArenaMessage.Dodge(
                    didArmorIncrease = didArmorIncrease,
                    dodgerUserId = defenderUserId,
                )
        )
    }
}
