package com.linklabs.sorcerersandspirits.ui.screens.arena

import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaMatch
import com.linklabs.sorcerersandspirits.domain.arena.model.ArenaPlayer
import com.linklabs.sorcerersandspirits.domain.arena.model.Gauntlet
import com.linklabs.sorcerersandspirits.domain.game.CharacterClassType
import com.linklabs.sorcerersandspirits.domain.profile.GameStats
import com.linklabs.sorcerersandspirits.domain.profile.ProfileStats

/**
 * Accumulates the local player's stat deltas across a single Arena session, which can span several
 * matches and Gauntlets, so the totals are written once when the player finally leaves (see
 * [consumeDelta]). The forfeit-versus-finished distinction lives here: a match that runs to its
 * Finished state is scored win/loss by [onMatchUpdate], whereas leaving with a match still in
 * progress is scored by [onLeave] as a loss (if the player was knocked out) or a forfeit.
 *
 * A Gauntlet is counted as won only when the player wins all [Gauntlet.MATCHES_PER_GAUNTLET]
 * matches, and its outcome is credited to a class only when the player used that same class for the
 * whole run. Per-class match outcomes are always credited to the class played that match.
 */
class ArenaSessionStats {

    private var accumulated = ProfileStats()

    private var lastSeenMatchNumber = 0
    private var currentMatchNumber = 0
    private var currentUserClass: CharacterClassType? = null
    private var currentMatchRecorded = false

    private var gauntletStartObserved = false
    private var gauntletWins = 0
    private var gauntletClasses = mutableSetOf<CharacterClassType>()
    private var participatedInGauntlet = false
    private var gauntletRecorded = false

    /** Returns whether this update recorded a new outcome, so a caller can persist the delta. */
    fun onMatchUpdate(match: ArenaMatch, userId: String): Boolean = recordingChange {
        if (match.matchNumberInGauntlet != lastSeenMatchNumber) {
            if (match.matchNumberInGauntlet == 1) startNewGauntlet()
            beginNewMatch(matchNumber = match.matchNumberInGauntlet)
            lastSeenMatchNumber = match.matchNumberInGauntlet
        }

        val userClass = (match.playerMap[userId] as? ArenaPlayer.Selected)?.characterClass?.type
        if (userClass != null) currentUserClass = userClass

        if (match is ArenaMatch.Finished && !currentMatchRecorded) {
            val userWon =
                (match.playerMap[userId] as? ArenaPlayer.Selected)?.characterClass?.isConscious() ==
                    true
            recordFinishedMatch(userWon = userWon)
        }
    }

    fun onClassSelected(classType: CharacterClassType): Boolean = recordingChange {
        accumulate(stats = GameStats(timesSelected = 1), classType = classType)
    }

    /**
     * Scores the leave: any match still in progress becomes a loss when [userKnockedOut] (the
     * player was already out) or a forfeit otherwise, and an unfinished Gauntlet the player took
     * part in is recorded as forfeited. A leave after the final match has finished records nothing
     * new.
     */
    fun onLeave(userKnockedOut: Boolean): Boolean = recordingChange {
        if (!currentMatchRecorded && currentUserClass != null) {
            val matchStats =
                if (userKnockedOut) {
                    GameStats(matchesPlayed = 1, matchesLost = 1)
                } else {
                    GameStats(matchesPlayed = 1, matchesForfeited = 1)
                }
            accumulate(stats = matchStats, classType = currentUserClass)
            currentMatchRecorded = true
            participatedInGauntlet = true
            currentUserClass?.let { gauntletClasses += it }
        }

        if (!gauntletRecorded && gauntletStartObserved && participatedInGauntlet) {
            accumulate(
                stats = GameStats(gauntletsPlayed = 1, gauntletsForfeited = 1),
                classType = gauntletClasses.singleOrNull(),
            )
            gauntletRecorded = true
        }
    }

    /** The delta accumulated so far, without resetting — for persisting a durable copy (SS-076). */
    fun peekDelta(): ProfileStats = accumulated

    /** Returns the accumulated delta and resets it, so a second flush writes nothing. */
    fun consumeDelta(): ProfileStats {
        val delta = accumulated
        accumulated = ProfileStats()
        return delta
    }

    private fun recordingChange(record: () -> Unit): Boolean {
        val before = accumulated
        record()
        return accumulated != before
    }

    private fun recordFinishedMatch(userWon: Boolean) {
        val matchStats =
            GameStats(
                matchesPlayed = 1,
                matchesWon = if (userWon) 1 else 0,
                matchesLost = if (userWon) 0 else 1,
            )
        accumulate(stats = matchStats, classType = currentUserClass)
        currentMatchRecorded = true
        participatedInGauntlet = true
        if (userWon) gauntletWins++
        currentUserClass?.let { gauntletClasses += it }

        val isFinalMatch = currentMatchNumber >= Gauntlet.MATCHES_PER_GAUNTLET
        if (isFinalMatch && gauntletStartObserved && !gauntletRecorded) {
            val wonGauntlet = gauntletWins >= Gauntlet.MATCHES_PER_GAUNTLET
            val gauntletStats =
                GameStats(
                    gauntletsPlayed = 1,
                    gauntletsWon = if (wonGauntlet) 1 else 0,
                    gauntletsLost = if (wonGauntlet) 0 else 1,
                )
            accumulate(stats = gauntletStats, classType = gauntletClasses.singleOrNull())
            gauntletRecorded = true
        }
    }

    private fun startNewGauntlet() {
        gauntletStartObserved = true
        gauntletWins = 0
        gauntletClasses = mutableSetOf()
        participatedInGauntlet = false
        gauntletRecorded = false
    }

    private fun beginNewMatch(matchNumber: Int) {
        currentMatchNumber = matchNumber
        currentMatchRecorded = false
        currentUserClass = null
    }

    private fun accumulate(stats: GameStats, classType: CharacterClassType?) {
        val perClass = if (classType == null) emptyMap() else mapOf(classType to stats)
        accumulated += ProfileStats(overall = stats, perClass = perClass)
    }
}
