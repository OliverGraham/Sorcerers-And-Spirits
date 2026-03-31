package com.linklabs.sorcerersandspirits.domain.traveler

import com.linklabs.sorcerersandspirits.domain.tavern.TavernStatus

data class Traveler(val userId: String, val displayName: String, val tavernStatus: TavernStatus)
