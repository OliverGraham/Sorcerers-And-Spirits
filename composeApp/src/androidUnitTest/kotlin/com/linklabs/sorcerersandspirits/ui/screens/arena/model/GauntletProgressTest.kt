package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import com.linklabs.sorcerersandspirits.domain.arena.model.Gauntlet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GauntletProgressTest {

    @Test
    fun `matches before the last are not the final match`() {
        for (currentMatch in 1 until Gauntlet.MATCHES_PER_GAUNTLET) {
            assertFalse(actual = GauntletProgress(currentMatch = currentMatch).isFinalMatch)
        }
    }

    @Test
    fun `the last match is the final match`() {
        assertTrue(
            actual = GauntletProgress(currentMatch = Gauntlet.MATCHES_PER_GAUNTLET).isFinalMatch
        )
    }

    @Test
    fun `defaults to the first match of a full gauntlet`() {
        val progress = GauntletProgress()

        assertEquals(expected = 1, actual = progress.currentMatch)
        assertEquals(expected = Gauntlet.MATCHES_PER_GAUNTLET, actual = progress.matchesPerGauntlet)
    }
}
