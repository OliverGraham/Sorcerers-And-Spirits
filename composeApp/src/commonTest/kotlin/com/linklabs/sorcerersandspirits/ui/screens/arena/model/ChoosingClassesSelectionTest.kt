package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassStates
import kotlin.test.Test
import kotlin.test.assertEquals

class ChoosingClassesSelectionTest {

    private val characterClasses = previewCharacterClassStates()

    private fun classOfType(type: CharacterClassType): CharacterClassState =
        characterClasses.first {
            it.type ==
                type
        }

    @Test
    fun `a previous selection taken by another player is kept, not moved to Rogue (SS-187)`() {
        val resolved =
            characterClasses.resolveSelectedClass(
                previousSelection = classOfType(type = CharacterClassType.Mage),
                unavailableClasses = setOf(CharacterClassType.Mage),
            )

        assertEquals(expected = CharacterClassType.Mage, actual = resolved?.type)
    }

    @Test
    fun `a still-available previous selection is kept`() {
        val resolved =
            characterClasses.resolveSelectedClass(
                previousSelection = classOfType(type = CharacterClassType.Mage),
                unavailableClasses = setOf(CharacterClassType.Rogue),
            )

        assertEquals(expected = CharacterClassType.Mage, actual = resolved?.type)
    }

    @Test
    fun `no previous selection defaults to the available Rogue`() {
        val resolved =
            characterClasses.resolveSelectedClass(
                previousSelection = null,
                unavailableClasses = emptySet(),
            )

        assertEquals(expected = CharacterClassType.Rogue, actual = resolved?.type)
    }
}
