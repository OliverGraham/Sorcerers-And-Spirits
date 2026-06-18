package com.linklabs.sorcerersandspirits.domain.profile

/** Supplies a random traveler name used as a placeholder display name before sign-in. */
interface RandomNameProvider {

    fun getRandomName(): String
}
