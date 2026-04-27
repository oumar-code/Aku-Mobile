package com.akuplatform.shared.api

import com.akuplatform.shared.auth.model.AuthToken
import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.course.model.Enrollment
import com.akuplatform.shared.course.model.Lesson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LoginRequest(val email: String, val password: String)

@Serializable
private data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
private data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
private data class ForgotPasswordRequest(val email: String)

@Serializable
private data class EnrollRequest(@SerialName("course_id") val courseId: String)

@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long
)

class Wave3ApiClient(
    private val baseUrl: String = BASE_URL,
    private val httpClient: HttpClient = defaultClient()
) {

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

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun TokenResponse.toAuthToken(): AuthToken = AuthToken(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = expiresIn,
        expiresAt = Clock.System.now().epochSeconds + expiresIn
    )

    /**
     * Wraps an API call, mapping transport exceptions to [ApiError.Network]
     * and non-2xx HTTP responses to [ApiError.Unauthorized] / [ApiError.ServerError].
     */
    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: ApiError) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(ApiError.Network(e.message ?: "Network error"))
    }

    private suspend fun HttpResponse.requireSuccess(): HttpResponse {
        return when (status) {
            HttpStatusCode.OK, HttpStatusCode.Created, HttpStatusCode.NoContent -> this
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                throw ApiError.Unauthorized()
            else -> throw ApiError.ServerError(status.value)
        }
    }

    /** Adds a Bearer [token] Authorization header when [token] is not null. */
    private fun io.ktor.client.request.HttpRequestBuilder.bearerAuth(token: String?) {
        if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
    }

    // ── public API ───────────────────────────────────────────────────────────

    suspend fun authenticate(email: String, password: String): Result<AuthToken> =
        safeCall {
            httpClient.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }.requireSuccess().body<TokenResponse>().toAuthToken()
        }

    suspend fun refreshToken(refreshToken: String): Result<AuthToken> =
        safeCall {
            httpClient.post("$baseUrl/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }.requireSuccess().body<TokenResponse>().toAuthToken()
        }

    suspend fun register(email: String, password: String, name: String): Result<AuthToken> =
        safeCall {
            httpClient.post("$baseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(email, password, name))
            }.requireSuccess().body<TokenResponse>().toAuthToken()
        }

    suspend fun requestPasswordReset(email: String): Result<Unit> =
        safeCall {
            httpClient.post("$baseUrl/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(ForgotPasswordRequest(email))
            }.requireSuccess()
            Unit
        }

    // ── course API ────────────────────────────────────────────────────────────

    suspend fun getCourses(token: String? = null): Result<List<Course>> =
        safeCall {
            httpClient.get("$baseUrl/courses") {
                bearerAuth(token)
            }.requireSuccess().body()
        }

    suspend fun getCourseById(id: String, token: String? = null): Result<Course> =
        safeCall {
            httpClient.get("$baseUrl/courses/$id") {
                bearerAuth(token)
            }.requireSuccess().body()
        }

    suspend fun getLessons(courseId: String, token: String? = null): Result<List<Lesson>> =
        safeCall {
            httpClient.get("$baseUrl/courses/$courseId/lessons") {
                bearerAuth(token)
            }.requireSuccess().body()
        }

    suspend fun getEnrolledCourses(token: String? = null): Result<List<Enrollment>> =
        safeCall {
            httpClient.get("$baseUrl/enrollments") {
                bearerAuth(token)
            }.requireSuccess().body()
        }

    suspend fun enrollInCourse(courseId: String, token: String? = null): Result<Enrollment> =
        safeCall {
            httpClient.post("$baseUrl/enrollments") {
                contentType(ContentType.Application.Json)
                bearerAuth(token)
                setBody(EnrollRequest(courseId))
            }.requireSuccess().body()
        }

    fun close() = httpClient.close()
}
