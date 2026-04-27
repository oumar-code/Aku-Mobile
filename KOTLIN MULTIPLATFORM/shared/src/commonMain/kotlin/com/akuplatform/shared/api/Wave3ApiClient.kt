package com.akuplatform.shared.api

import com.akuplatform.shared.auth.model.AuthToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LoginRequest(val email: String, val password: String)

@Serializable
private data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long
)

class Wave3ApiClient internal constructor(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {

    constructor(baseUrl: String = BASE_URL) : this(baseUrl, defaultClient())

    companion object {
        const val BASE_URL = "https://api.akulearn.com/v3"

        internal fun defaultClient() = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.HEADERS
            }
        }
    }

    suspend fun authenticate(email: String, password: String): Result<AuthToken> =
        runCatching {
            val response: TokenResponse = httpClient.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }.body()
            AuthToken(response.accessToken, response.refreshToken, response.expiresIn)
        }

    suspend fun refreshToken(refreshToken: String): Result<AuthToken> =
        runCatching {
            val response: TokenResponse = httpClient.post("$baseUrl/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }.body()
            AuthToken(response.accessToken, response.refreshToken, response.expiresIn)
        }

    fun close() = httpClient.close()
}
