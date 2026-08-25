package com.linklabs.sorcerersandspirits.domain.computer

import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

data class ComputerOpponent(val userId: String, val displayName: String) {
    fun toTraveler(): Traveler = Traveler(
        userId = userId,
        displayName = displayName,
        tavernStatus = TavernStatus.Available,
        // Computer opponents have no profile and so no rating to show.
        rating = 0,
    )
}

object ComputerOpponents {
    const val ID_PREFIX = "computer-"

    val default = ComputerOpponent(userId = "${ID_PREFIX}automaton", displayName = "The Automaton")

    val all: List<ComputerOpponent> = listOf(default)

    fun isComputer(userId: String): Boolean = userId.startsWith(prefix = ID_PREFIX)
}
