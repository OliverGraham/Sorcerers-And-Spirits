package com.linklabs.sorcerersandspirits.domain.profile

data class Profile(
    val userId: String,
    val displayName: String,
    val stats: Map<String, Long> = emptyMap(),
)
