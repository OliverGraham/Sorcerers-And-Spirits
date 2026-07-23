package com.linklabs.sorcerersandspirits.ui.screens.arena.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CombatMomentHoldTest {

    @Test
    fun `Given no resolution is animating, then the live moment is displayed as it advances`() {
        val hold =
            CombatMomentHold.initial(liveMoment = actor(ACTOR), latestAnimationMessageId = null)

        val next =
            hold.advancedWith(
                liveMoment = actor(NEXT_ACTOR),
                latestAnimationMessageId = null,
                isSequencing = false,
            )

        assertEquals(expected = actor(NEXT_ACTOR), actual = next.displayedMoment)
    }

    @Test
    fun `Given a resolution lands, then the acting moment holds in the same step the live moment advances`() {
        val idle =
            CombatMomentHold.initial(liveMoment = actor(ACTOR), latestAnimationMessageId = "m0")

        val landed =
            idle.advancedWith(
                liveMoment = actor(NEXT_ACTOR),
                latestAnimationMessageId = "m1",
                isSequencing = false,
            )

        assertEquals(expected = actor(ACTOR), actual = landed.displayedMoment)
    }

    @Test
    fun `Given the resolution is still sequencing, then the acting moment stays displayed`() {
        val landed =
            CombatMomentHold.initial(liveMoment = actor(ACTOR), latestAnimationMessageId = "m0")
                .advancedWith(
                    liveMoment = actor(NEXT_ACTOR),
                    latestAnimationMessageId = "m1",
                    isSequencing = false,
                )

        val sequencing =
            landed.advancedWith(
                liveMoment = actor(NEXT_ACTOR),
                latestAnimationMessageId = "m1",
                isSequencing = true,
            )

        assertEquals(expected = actor(ACTOR), actual = sequencing.displayedMoment)
    }

    @Test
    fun `Given the sequencer finishes, then the next moment is adopted`() {
        val sequencing =
            CombatMomentHold.initial(liveMoment = actor(ACTOR), latestAnimationMessageId = "m0")
                .advancedWith(
                    liveMoment = actor(NEXT_ACTOR),
                    latestAnimationMessageId = "m1",
                    isSequencing = false,
                )
                .advancedWith(
                    liveMoment = actor(NEXT_ACTOR),
                    latestAnimationMessageId = "m1",
                    isSequencing = true,
                )

        val finished =
            sequencing.advancedWith(
                liveMoment = actor(NEXT_ACTOR),
                latestAnimationMessageId = "m1",
                isSequencing = false,
            )

        assertEquals(expected = actor(NEXT_ACTOR), actual = finished.displayedMoment)
    }

    @Test
    fun `Given the full resolution runs, then the moment never advances before the sequencer finishes`() {
        var hold =
            CombatMomentHold.initial(liveMoment = actor(ACTOR), latestAnimationMessageId = "m0")

        val landedThenSequencing =
            listOf(false, true, true, true).map { isSequencing ->
                hold =
                    hold.advancedWith(
                        liveMoment = actor(NEXT_ACTOR),
                        latestAnimationMessageId = "m1",
                        isSequencing = isSequencing,
                    )
                hold.displayedMoment
            }

        landedThenSequencing.forEach { moment ->
            assertEquals(expected = actor(ACTOR), actual = moment)
        }

        hold =
            hold.advancedWith(
                liveMoment = actor(NEXT_ACTOR),
                latestAnimationMessageId = "m1",
                isSequencing = false,
            )
        assertEquals(expected = actor(NEXT_ACTOR), actual = hold.displayedMoment)
    }

    private fun actor(userId: String): CombatMoment =
        CombatMoment(
            seating =
                CombatSeating(
                    topSeatUserId = userId,
                    bottomSeatUserId = null,
                    headIconUserIds = emptyList(),
                ),
            activeUserIds = setOf(element = userId),
            turnOwnerUserId = userId,
        )

    private companion object {
        const val ACTOR = "actor"
        const val NEXT_ACTOR = "nextActor"
    }
}
