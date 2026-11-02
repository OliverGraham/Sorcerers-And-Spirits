package com.linklabs.sorcerersandspirits.domain.arena.computeropponent

import com.linklabs.sorcerersandspirits.domain.arena.model.AbilityAction
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.NormalAttackType
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.game.MirrorImageRerollSelection
import com.linklabs.sorcerersandspirits.domain.game.ability.AbilityConfigProvider
import com.linklabs.sorcerersandspirits.domain.game.ability.DefensiveAbility
import com.linklabs.sorcerersandspirits.domain.game.usecase.GetCharacterClassesUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ComputerOpponentHandlerTest {

    private val rogue = characterClass(type = CharacterClassType.Rogue)
    private val gambler = characterClass(type = CharacterClassType.Gambler)

    private val getCharacterClassesUseCase = mockk<GetCharacterClassesUseCase>()

    private val handler =
        ComputerOpponentHandler(
            computerOpponentIdentity = ComputerOpponentIdentity(),
            getCharacterClassesUseCase = getCharacterClassesUseCase,
            abilityConfigProvider = mockk<AbilityConfigProvider>(relaxed = true),
        )

    init {
        every { getCharacterClassesUseCase() } returns listOf(rogue, gambler)
    }

    @Test
    fun `no turn is owed while the human is choosing a class`() = runTest {
        handler.start(userDisplayName = "You", userId = HUMAN_USER_ID)

        assertNull(handler.decideTurn())
    }

    @Test
    fun `computer chooses an available class once it is its turn`() = runTest {
        handler.start(userDisplayName = "You", userId = HUMAN_USER_ID)
        handler.chooseCharacterClass(
            selectedArenaPlayer =
            ArenaPlayer.Selected(userId = HUMAN_USER_ID, displayName = "You", characterClass = rogue),
            allPlayers =
            listOf(
                ArenaPlayer.Unselected(userId = COMPUTER_OPPONENT_USER_ID, displayName = "NPC"),
                ArenaPlayer.Selected(userId = HUMAN_USER_ID, displayName = "You", characterClass = rogue),
            ),
        )

        val decision = handler.decideTurn()

        val chooseClass = assertIs<ComputerOpponentDecision.ChooseClass>(decision)
        assertEquals(COMPUTER_OPPONENT_USER_ID, chooseClass.userId)
        assertEquals(CharacterClassType.Gambler, chooseClass.characterClass.type)
    }

    @Test
    fun `mirror image reroll targets the higher of the two dice`() {
        val mirrorImage =
            AbilityAction.Passive.MirrorImage(
                firstDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 2),
                secondDie = AbilityAction.Passive.MirrorImage.MirrorImageDie(originalRoll = 6),
                attackType = NormalAttackType.ArmorClass,
                existingDoublesBonus = 0,
                mirrorImageDefensiveAbility = DefensiveAbility.MirrorImage(),
            )

        val rerolled = assertIs<AbilityAction.Passive.MirrorImage>(mirrorImage.rerollingHigherMirrorImageDie())

        assertEquals(MirrorImageRerollSelection.Second, rerolled.mirrorImageRerollSelection)
    }

    private fun characterClass(type: CharacterClassType) = CharacterClass.create(
        type = type,
        hitPoints = 10,
        maxHitPoints = 10,
        armorClass = 10,
        speed = 5,
    )

    private companion object {
        const val HUMAN_USER_ID = "human"
    }
}
