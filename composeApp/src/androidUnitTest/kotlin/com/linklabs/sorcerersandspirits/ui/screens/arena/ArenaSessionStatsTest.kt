package com.linklabs.sorcerersandspirits.ui.screens.arena

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.game.CharacterClass
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArenaSessionStatsTest {

    @Test
    fun `a finished match is recorded once as a win for the user's class`() {
        val sessionStats = ArenaSessionStats()
        val finished = finishedMatch(matchNumber = 1, userClass = CharacterClass.getRogue())

        // Re-emitted twice (e.g. a follow-up message) but only scored once.
        sessionStats.onMatchUpdate(match = finished, userId = USER_ID)
        sessionStats.onMatchUpdate(match = finished, userId = USER_ID)

        assertEquals(
            expected =
                ProfileStats(
                    overall = GameStats(matchesPlayed = 1, matchesWon = 1),
                    perClass =
                        mapOf(
                            CharacterClassType.Rogue to GameStats(matchesPlayed = 1, matchesWon = 1)
                        ),
                ),
            actual = sessionStats.consumeDelta(),
        )
    }

    @Test
    fun `a finished match the user did not survive is a loss`() {
        val sessionStats = ArenaSessionStats()

        sessionStats.onMatchUpdate(
            match = finishedMatch(matchNumber = 1, userWon = false),
            userId = USER_ID,
        )

        val delta = sessionStats.consumeDelta()
        assertEquals(expected = 1, actual = delta.overall.matchesLost)
        assertEquals(expected = 0, actual = delta.overall.matchesWon)
    }

    @Test
    fun `winning all three matches of a gauntlet with one class records a gauntlet win`() {
        val sessionStats = ArenaSessionStats()

        (1..3).forEach { matchNumber ->
            sessionStats.onMatchUpdate(
                match =
                    finishedMatch(matchNumber = matchNumber, userClass = CharacterClass.getMage()),
                userId = USER_ID,
            )
        }

        val delta = sessionStats.consumeDelta()
        assertEquals(expected = 3, actual = delta.overall.matchesWon)
        assertEquals(expected = 1, actual = delta.overall.gauntletsPlayed)
        assertEquals(expected = 1, actual = delta.overall.gauntletsWon)
        assertEquals(
            expected = 1,
            actual = delta.perClass.getValue(CharacterClassType.Mage).gauntletsWon,
        )
    }

    @Test
    fun `losing a match in the gauntlet records a gauntlet loss`() {
        val sessionStats = ArenaSessionStats()

        sessionStats.onMatchUpdate(finishedMatch(matchNumber = 1, userWon = true), USER_ID)
        sessionStats.onMatchUpdate(finishedMatch(matchNumber = 2, userWon = false), USER_ID)
        sessionStats.onMatchUpdate(finishedMatch(matchNumber = 3, userWon = true), USER_ID)

        val delta = sessionStats.consumeDelta()
        assertEquals(expected = 1, actual = delta.overall.gauntletsLost)
        assertEquals(expected = 0, actual = delta.overall.gauntletsWon)
    }

    @Test
    fun `a gauntlet won with mixed classes is credited only to overall, not per-class`() {
        val sessionStats = ArenaSessionStats()

        sessionStats.onMatchUpdate(finishedMatch(1, CharacterClass.getRogue()), USER_ID)
        sessionStats.onMatchUpdate(finishedMatch(2, CharacterClass.getBard()), USER_ID)
        sessionStats.onMatchUpdate(finishedMatch(3, CharacterClass.getMage()), USER_ID)

        val delta = sessionStats.consumeDelta()
        assertEquals(expected = 1, actual = delta.overall.gauntletsWon)
        assertTrue(delta.perClass.values.all { it.gauntletsWon == 0L })
    }

    @Test
    fun `leaving a match in progress forfeits the match and gauntlet`() {
        val sessionStats = ArenaSessionStats()
        sessionStats.onMatchUpdate(
            choosingMatch(matchNumber = 1, CharacterClass.getDuelist()),
            USER_ID,
        )

        sessionStats.onLeave(userKnockedOut = false)

        val delta = sessionStats.consumeDelta()
        assertEquals(expected = 1, actual = delta.overall.matchesForfeited)
        assertEquals(expected = 1, actual = delta.overall.gauntletsForfeited)
        assertEquals(
            expected = 1,
            actual = delta.perClass.getValue(CharacterClassType.Duelist).matchesForfeited,
        )
    }

    @Test
    fun `leaving while knocked out records a loss but a forfeited gauntlet`() {
        val sessionStats = ArenaSessionStats()
        sessionStats.onMatchUpdate(
            choosingMatch(matchNumber = 1, CharacterClass.getRogue()),
            USER_ID,
        )

        sessionStats.onLeave(userKnockedOut = true)

        val delta = sessionStats.consumeDelta()
        assertEquals(expected = 1, actual = delta.overall.matchesLost)
        assertEquals(expected = 0, actual = delta.overall.matchesForfeited)
        assertEquals(expected = 1, actual = delta.overall.gauntletsForfeited)
    }

    @Test
    fun `leaving after the gauntlet has finished records nothing new`() {
        val sessionStats = ArenaSessionStats()
        (1..3).forEach {
            sessionStats.onMatchUpdate(finishedMatch(matchNumber = it, userWon = true), USER_ID)
        }

        sessionStats.onLeave(userKnockedOut = false)

        val delta = sessionStats.consumeDelta()
        assertEquals(expected = 0, actual = delta.overall.matchesForfeited)
        assertEquals(expected = 0, actual = delta.overall.gauntletsForfeited)
        assertEquals(expected = 3, actual = delta.overall.matchesPlayed)
    }

    @Test
    fun `joining mid-gauntlet does not record a forfeited gauntlet on leave`() {
        val sessionStats = ArenaSessionStats()
        sessionStats.onMatchUpdate(
            choosingMatch(matchNumber = 2, CharacterClass.getRogue()),
            USER_ID,
        )

        sessionStats.onLeave(userKnockedOut = false)

        val delta = sessionStats.consumeDelta()
        assertEquals(expected = 1, actual = delta.overall.matchesForfeited)
        assertEquals(expected = 0, actual = delta.overall.gauntletsForfeited)
    }

    @Test
    fun `selecting a class records timesSelected`() {
        val sessionStats = ArenaSessionStats()

        sessionStats.onClassSelected(classType = CharacterClassType.Bard)

        val delta = sessionStats.consumeDelta()
        assertEquals(expected = 1, actual = delta.overall.timesSelected)
        assertEquals(
            expected = 1,
            actual = delta.perClass.getValue(CharacterClassType.Bard).timesSelected,
        )
    }

    @Test
    fun `consumeDelta resets the accumulator`() {
        val sessionStats = ArenaSessionStats()
        sessionStats.onClassSelected(classType = CharacterClassType.Bard)

        sessionStats.consumeDelta()

        assertTrue(sessionStats.consumeDelta().isEmpty)
    }

    private fun finishedMatch(
        matchNumber: Int,
        userClass: CharacterClass = CharacterClass.getRogue(),
        userWon: Boolean = true,
    ): ArenaMatch.Finished {
        val survivingClass = if (userWon) userClass else userClass.copy(hitPoints = 0)
        return ArenaMatch.Finished(
            messages = emptyList(),
            numberOfNewMessages = 0,
            playerMap =
                mapOf(
                    USER_ID to selectedPlayer(userId = USER_ID, characterClass = survivingClass),
                    OPPONENT_ID to
                        selectedPlayer(
                            userId = OPPONENT_ID,
                            characterClass = CharacterClass.getMage().copy(hitPoints = 0),
                        ),
                ),
            matchNumberInGauntlet = matchNumber,
        )
    }

    private fun choosingMatch(
        matchNumber: Int,
        userClass: CharacterClass,
    ): ArenaMatch.ChoosingClasses {
        val userPlayer = selectedPlayer(userId = USER_ID, characterClass = userClass)
        return ArenaMatch.ChoosingClasses(
            messages = emptyList(),
            numberOfNewMessages = 0,
            playerMap = mapOf(USER_ID to userPlayer),
            activePlayer = userPlayer,
            activePlayerUserId = USER_ID,
            matchNumberInGauntlet = matchNumber,
        )
    }

    private fun selectedPlayer(userId: String, characterClass: CharacterClass) =
        ArenaPlayer.Selected(userId = userId, displayName = userId, characterClass = characterClass)

    private companion object {
        const val USER_ID = "user-1"
        const val OPPONENT_ID = "opponent-1"
    }
}
