package com.linklabs.sorcerersandspirits.data.bugreport

import com.linklabs.sorcerersandspirits.domain.bugreport.BugReport
import kotlinx.serialization.Serializable

@Serializable
data class BugReportRequestEntity(
    val token: String,
    val message: String,
    val userId: String,
    val displayName: String,
    val appVersion: String,
    val platform: String,
)

internal fun BugReport.toEntity(token: String) = BugReportRequestEntity(
    token = token,
    message = message,
    userId = userId,
    displayName = displayName,
    appVersion = appVersion,
    platform = platform,
)
