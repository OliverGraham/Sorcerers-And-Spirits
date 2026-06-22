package com.linklabs.sorcerersandspirits.domain.computer

import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

/**
 * A non-human opponent the user can challenge from the Tavern and fight in the Arena. The computer
 * has no client of its own: the challenging user's client seeds it into the match and drives its
 * decisions after a short "thinking" delay.
 */
data class ComputerOpponent(val userId: String, val displayName: String) {
    fun toTraveler(): Traveler =
        Traveler(
            userId = userId,
            displayName = displayName,
            tavernStatus = TavernStatus.Available,
            // Computer opponents have no profile and so no rating to show.
            rating = 0,
        )
}

/**
 * Registry of the computer opponents available to challenge. Only a single [default] bot exists for
 * now; the [ID_PREFIX] convention lets more bots be added and lets any layer tell a computer player
 * apart from a human one by id alone.
 */
object ComputerOpponents {
    const val ID_PREFIX = "computer-"

    val default = ComputerOpponent(userId = "${ID_PREFIX}automaton", displayName = "The Automaton")

    val all: List<ComputerOpponent> = listOf(default)

    fun isComputer(userId: String): Boolean = userId.startsWith(prefix = ID_PREFIX)
}
