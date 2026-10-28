package com.linklabs.sorcerersandspirits.data.authentication

import com.linklabs.sorcerersandspirits.data.config.RemoteConfigDataSource
import com.linklabs.sorcerersandspirits.domain.authentication.AppleTokenRevoker
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AppsScriptAppleTokenRevoker(
    private val httpClient: HttpClient,
    private val remoteConfigDataSource: RemoteConfigDataSource,
) : AppleTokenRevoker {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun revoke(authorizationCode: String): Boolean {
        val endpointUrl = remoteConfigDataSource.appleRevokeEndpointUrl()
        if (endpointUrl.isBlank()) return false

        val requestBody = AppleRevokeRequestEntity(
            token = remoteConfigDataSource.appleRevokeToken(),
            authorizationCode = authorizationCode,
        )

        return runCatching {
            val postResponse = httpClient.post(urlString = endpointUrl) {
                contentType(type = ContentType.Application.Json)
                setBody(body = json.encodeToString(value = requestBody))
            }

            val body = readBody(postResponse = postResponse)
            json.decodeFromString<AppleRevokeResponseEntity>(string = body).success
        }
            .getOrElse { false }
    }

    private suspend fun readBody(postResponse: HttpResponse): String {
        val redirectLocation = postResponse.headers[HttpHeaders.Location]

        return if (redirectLocation != null) {
            httpClient.get(urlString = redirectLocation).bodyAsText()
        } else {
            postResponse.bodyAsText()
        }
    }
}

@Serializable
private data class AppleRevokeRequestEntity(
    val token: String,
    val authorizationCode: String,
)

@Serializable
private data class AppleRevokeResponseEntity(
    val success: Boolean = false,
)
