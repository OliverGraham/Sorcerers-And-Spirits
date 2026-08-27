package com.linklabs.sorcerersandspirits.data.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.AttackTurn
import com.linklabs.sorcerersandspirits.domain.arena.model.NextCombatAction
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.MirrorImageRerollSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NextCombatActionEntityTest {

    private fun mirrorImageAction(mirrorImageRerollSelection: MirrorImageRerollSelection) =
        NextCombatAction.AbilityRoll(
            purpose =
            AbilityAction.Passive.MirrorImage(
                firstDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 4),
                secondDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 6),
                attackType = NormalAttackType.ArmorClass,
                existingDoublesBonus = 0,
                mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
                mirrorImageRerollSelection = mirrorImageRerollSelection,
            ),
            attackTurn = AttackTurn.Standard(attackerUserId = "attacker"),
            defenderUserId = "defender",
            activeUserIds = setOf("defender"),
        )

    @Test
    fun `Mirror Image rerollSelection survives the entity round-trip`() {
        val roundTripped =
            mirrorImageAction(mirrorImageRerollSelection = MirrorImageRerollSelection.Second)
                .toEntity()
                .toDomain()

        val abilityRoll = assertIs<NextCombatAction.AbilityRoll>(roundTripped)
        val mirrorImage = assertIs<AbilityAction.Passive.MirrorImage>(abilityRoll.purpose)
        assertEquals(MirrorImageRerollSelection.Second, mirrorImage.mirrorImageRerollSelection)
    }
}
