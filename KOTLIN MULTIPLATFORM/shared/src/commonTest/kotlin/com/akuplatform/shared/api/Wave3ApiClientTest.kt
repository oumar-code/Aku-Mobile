package com.akuplatform.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Wave3ApiClientTest {

    private val successBody =
        """{"access_token":"acc","refresh_token":"ref","expires_in":7200}"""

    private fun clientWith(status: HttpStatusCode, body: String): Wave3ApiClient {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return Wave3ApiClient(httpClient = http)
    }

    @Test
    fun `authenticate returns AuthToken on 200 response`() = runTest {
        val client = clientWith(HttpStatusCode.OK, successBody)
        val result = client.authenticate("user@example.com", "secret")
        assertTrue(result.isSuccess)
        val token = result.getOrThrow()
        assertEquals("acc", token.accessToken)
        assertEquals("ref", token.refreshToken)
        assertEquals(7200L, token.expiresIn)
        assertTrue(token.expiresAt > 0L, "expiresAt should be computed")
    }

    @Test
    fun `authenticate returns ApiError Unauthorized on 401 response`() = runTest {
        val client = clientWith(HttpStatusCode.Unauthorized, """{"error":"bad_credentials"}""")
        val result = client.authenticate("user@example.com", "wrong")
        assertTrue(result.isFailure)
        assertIs<ApiError.Unauthorized>(result.exceptionOrNull())
    }

    @Test
    fun `authenticate returns ApiError ServerError on 500 response`() = runTest {
        val client = clientWith(HttpStatusCode.InternalServerError, """{"error":"server_error"}""")
        val result = client.authenticate("user@example.com", "pass")
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<ApiError.ServerError>(error)
        assertEquals(500, (error as ApiError.ServerError).code)
    }

    @Test
    fun `refreshToken returns AuthToken on 200 response`() = runTest {
        val client = clientWith(HttpStatusCode.OK, successBody)
        val result = client.refreshToken("old-refresh-token")
        assertTrue(result.isSuccess)
        assertEquals("acc", result.getOrThrow().accessToken)
    }

    @Test
    fun `refreshToken returns ApiError Unauthorized on 401 response`() = runTest {
        val client = clientWith(HttpStatusCode.Unauthorized, """{"error":"token_expired"}""")
        val result = client.refreshToken("expired-token")
        assertTrue(result.isFailure)
        assertIs<ApiError.Unauthorized>(result.exceptionOrNull())
    }

    @Test
    fun `register returns AuthToken on 200 response`() = runTest {
        val client = clientWith(HttpStatusCode.OK, successBody)
        val result = client.register("user@example.com", "pass", "Test User")
        assertTrue(result.isSuccess)
        assertEquals("acc", result.getOrThrow().accessToken)
    }

    @Test
    fun `requestPasswordReset returns success on 200 response`() = runTest {
        val client = clientWith(HttpStatusCode.OK, """{}""")
        val result = client.requestPasswordReset("user@example.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `requestPasswordReset returns ApiError ServerError on 500 response`() = runTest {
        val client = clientWith(HttpStatusCode.InternalServerError, """{"error":"server_error"}""")
        val result = client.requestPasswordReset("user@example.com")
        assertTrue(result.isFailure)
        assertIs<ApiError.ServerError>(result.exceptionOrNull())
    }
}
