package com.linklabs.sorcerersandspirits.domain.version

/** Reads the running build's version code (Android versionCode / iOS CFBundleVersion). */
interface CurrentAppVersionProvider {
    fun versionCode(): Long
}
