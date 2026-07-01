package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.CharacterClassState
import com.linklabs.sorcerersandspirits.ui.common.characterclasses.previewCharacterClassStates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChoosingClassesSelectionTest {

    private val characterClasses = previewCharacterClassStates()

    private fun classOfType(type: CharacterClassType): CharacterClassState =
        characterClasses.first { it.type == type }

    @Test
    fun `a previous selection taken by another player is dropped, never re-selected (SS-187)`() {
        val resolved =
            characterClasses.resolveSelectedClass(
                previousSelection = classOfType(type = CharacterClassType.Rogue),
                unavailableClasses = setOf(CharacterClassType.Rogue),
            )

        assertNotNull(actual = resolved)
        assertTrue(actual = resolved.type != CharacterClassType.Rogue)
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
