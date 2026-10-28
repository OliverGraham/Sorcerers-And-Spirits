package com.linklabs.sorcerersandspirits.data.config

import kotlinx.serialization.Serializable

@Serializable
data class AppleRevokeConfigEntity(
    val endpointUrl: String,
    val token: String,
) {
    companion object {
        val EMPTY = AppleRevokeConfigEntity(endpointUrl = "", token = "")
    }
}
