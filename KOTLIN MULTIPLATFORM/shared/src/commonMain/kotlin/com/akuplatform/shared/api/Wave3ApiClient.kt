package com.akuplatform.shared.api

import com.akuplatform.shared.auth.model.AuthToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
private data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
private data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long
)

class Wave3ApiClient(
    private val baseUrl: String = BASE_URL,
    private val httpClient: HttpClient = buildDefaultClient()
) {

    companion object {
        const val BASE_URL = "https://api.akulearn.com/v3"

        private fun buildDefaultClient() = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    suspend fun authenticate(email: String, password: String): Result<AuthToken> {
        return try {
            val response: AuthResponse = httpClient.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }.body()
            Result.success(
                AuthToken(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    expiresIn = response.expiresIn
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(refreshToken: String): Result<AuthToken> {
        return try {
            val response: AuthResponse = httpClient.post("$baseUrl/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }.body()
            Result.success(
                AuthToken(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    expiresIn = response.expiresIn
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
