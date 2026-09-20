package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.data.config.RemoteConfigDataSource.Companion.UNGATED_VERSION
import kotlinx.serialization.Serializable

@Serializable
data class VersionGateEntity(
    val minimumSupportedVersionCode: Long,
    val latestVersionCode: Long,
) {
    companion object {
        val UNGATED = VersionGateEntity(
            minimumSupportedVersionCode = UNGATED_VERSION,
            latestVersionCode = UNGATED_VERSION,
        )
    }
}
