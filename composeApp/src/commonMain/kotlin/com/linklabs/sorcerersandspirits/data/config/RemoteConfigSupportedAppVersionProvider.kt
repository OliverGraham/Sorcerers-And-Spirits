package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.version.SupportedAppVersionProvider

class RemoteConfigSupportedAppVersionProvider(
    private val remoteConfigDataSource: RemoteConfigDataSource,
) : SupportedAppVersionProvider {

    override fun minimumSupportedVersionCode(): Long = remoteConfigDataSource.minimumSupportedVersionCode()

    override fun latestVersionCode(): Long = remoteConfigDataSource.latestVersionCode()
}
