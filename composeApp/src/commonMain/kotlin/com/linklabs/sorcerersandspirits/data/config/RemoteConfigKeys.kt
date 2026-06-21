package com.linklabs.sorcerersandspirits.data.config

object RemoteConfigKeys {
    const val CHARACTER_CLASS_STATS = "character_class_stats"
    const val ABILITY_CONFIG = "ability_config"
    const val CHARACTER_CLASS_COPY = "character_class_copy"
    const val RANDOM_TRAVELER_NAMES = "random_traveler_names"

    // Lowest build version code still allowed to run against the shared RTDB schema. Clients below
    // it are force-upgraded so a stale client can't read/write incompatible game state.
    const val MINIMUM_SUPPORTED_VERSION = "minimum_supported_version"

    // Latest published build version code. Clients below it (but at/above the minimum) get a
    // dismissible "update available" nudge.
    const val LATEST_VERSION = "latest_version"
}
