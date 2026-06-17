package com.linklabs.sorcerersandspirits.domain.arena.model

/**
 * A Gauntlet is a fixed run of [MATCHES_PER_GAUNTLET] consecutive matches in the same Arena. It
 * sits above a single Match in the game-terms hierarchy (Turn -> Round -> Match -> Gauntlet) and is
 * the unit of play that a single stats write is attributed to.
 */
object Gauntlet {
    const val MATCHES_PER_GAUNTLET = 3
}
