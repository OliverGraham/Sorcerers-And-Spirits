package com.linklabs.sorcerersandspirits.domain.tavern

import com.linklabs.sorcerersandspirits.domain.profile.Profile
import com.linklabs.sorcerersandspirits.domain.traveler.Traveler

data class TavernData(
    val profile: Profile,
    val travelers: List<Traveler>,
    val challenge: Challenge?,
)
