package com.linklabs.sorcerersandspirits.data.bugreport

import com.linklabs.sorcerersandspirits.data.config.RemoteConfigDataSource
import com.linklabs.sorcerersandspirits.domain.bugreport.BugReport
import com.linklabs.sorcerersandspirits.domain.bugreport.BugReportDataSource
import com.linklabs.sorcerersandspirits.domain.bugreport.BugReportResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class AppsScriptBugReportDataSource(
    private val httpClient: HttpClient,
    private val remoteConfigDataSource: RemoteConfigDataSource,
) : BugReportDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun submit(report: BugReport): BugReportResult {
        val endpointUrl = remoteConfigDataSource.bugReportEndpointUrl()
        if (endpointUrl.isBlank()) return BugReportResult.Failure(message = null)

        val requestBody = report.toEntity(token = remoteConfigDataSource.bugReportToken())

        return runCatching {
            val postResponse = httpClient.post(urlString = endpointUrl) {
                contentType(type = ContentType.Application.Json)
                setBody(body = json.encodeToString(value = requestBody))
            }

            val body = readBody(postResponse = postResponse)
            val response = json.decodeFromString<BugReportResponseEntity>(string = body)
            if (response.success) {
                BugReportResult.Success
            } else {
                BugReportResult.Failure(message = response.message)
            }
        }
            .getOrElse { BugReportResult.Failure(message = null) }
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
