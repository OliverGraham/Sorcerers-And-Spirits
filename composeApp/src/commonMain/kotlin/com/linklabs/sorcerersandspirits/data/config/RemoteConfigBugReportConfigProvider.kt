package com.linklabs.sorcerersandspirits.data.config

import com.linklabs.sorcerersandspirits.domain.bugreport.BugReportConfigProvider

class RemoteConfigBugReportConfigProvider(
    private val remoteConfigDataSource: RemoteConfigDataSource,
) : BugReportConfigProvider {

    override fun bugReportEndpointUrl(): String = remoteConfigDataSource.bugReportEndpointUrl()
}
