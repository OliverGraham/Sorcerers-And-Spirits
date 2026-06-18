package com.linklabs.sorcerersandspirits.data.version

import com.linklabs.sorcerersandspirits.domain.version.CurrentAppVersionProvider
import platform.Foundation.NSBundle

class IosCurrentAppVersionProvider : CurrentAppVersionProvider {

    // CFBundleVersion is the integer build number (CURRENT_PROJECT_VERSION), the iOS analogue of
    // Android's versionCode. Falls back to 0 so a missing key never force-upgrades the user.
    override fun versionCode(): Long {
        val bundleVersion =
            NSBundle.mainBundle.objectForInfoDictionaryKey(key = "CFBundleVersion") as? String

        return bundleVersion?.toLongOrNull() ?: 0L
    }
}
