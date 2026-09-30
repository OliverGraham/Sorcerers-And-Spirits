package com.linklabs.sorcerersandspirits.data.bugreport

import kotlinx.serialization.Serializable

@Serializable
data class BugReportResponseEntity(
    val success: Boolean,
    val message: String? = null,
)
