package com.linklabs.sorcerersandspirits.domain.version

enum class AppUpgradeStatus {
    /** Build is at or above the latest published version. */
    UP_TO_DATE,

    /** A newer build exists; the user is nudged but can keep playing. */
    UPDATE_AVAILABLE,

    /** Build is below the minimum supported version and must update before continuing. */
    FORCE_UPGRADE,
}
