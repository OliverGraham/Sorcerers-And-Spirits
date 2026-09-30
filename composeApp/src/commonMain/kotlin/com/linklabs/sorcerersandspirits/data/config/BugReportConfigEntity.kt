package com.linklabs.sorcerersandspirits.data.config

import kotlinx.serialization.Serializable

@Serializable
data class BugReportConfigEntity(
    val endpointUrl: String,
    val token: String,
) {
    companion object {
        val EMPTY = BugReportConfigEntity(endpointUrl = "", token = "")
    }
}
