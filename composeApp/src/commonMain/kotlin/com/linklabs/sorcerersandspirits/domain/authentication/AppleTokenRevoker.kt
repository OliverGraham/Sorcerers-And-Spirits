package com.linklabs.sorcerersandspirits.domain.authentication

interface AppleTokenRevoker {
    suspend fun revoke(authorizationCode: String): Boolean
}
