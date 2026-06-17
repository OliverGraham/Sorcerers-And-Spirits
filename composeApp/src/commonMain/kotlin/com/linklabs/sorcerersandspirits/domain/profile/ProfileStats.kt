package com.linklabs.sorcerersandspirits.domain.profile

data class ProfileStats(val wins: Long = 0, val losses: Long = 0) {
    val isEmpty: Boolean
        get() = wins == 0L && losses == 0L
}
