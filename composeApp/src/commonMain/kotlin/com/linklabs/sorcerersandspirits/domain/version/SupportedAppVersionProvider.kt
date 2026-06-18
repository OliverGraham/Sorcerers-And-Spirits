package com.linklabs.sorcerersandspirits.domain.version

/**
 * The version thresholds published by the backend (RemoteConfig): the lowest build still allowed to
 * run, and the latest published build. Implemented in the data layer over RemoteConfig.
 */
interface SupportedAppVersionProvider {

    /** Builds below this are force-upgraded. */
    fun minimumSupportedVersionCode(): Long

    /** Builds below this (but at/above the minimum) get a dismissible update nudge. */
    fun latestVersionCode(): Long
}
