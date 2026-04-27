package com.akuplatform.shared.auth

import com.akuplatform.shared.api.Wave3ApiClient
import com.akuplatform.shared.auth.model.AuthToken
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
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryTest {

    private lateinit var storage: FakeTokenStorage
    private lateinit var sessionManager: SessionManager

    /** Builds a Wave3ApiClient backed by a [MockEngine] that always succeeds. */
    private fun successClient(): Wave3ApiClient {
        val engine = MockEngine {
            respond(
                content = """{"access_token":"tok","refresh_token":"ref","expires_in":3600}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return Wave3ApiClient(httpClient = client)
    }

    /** Builds a Wave3ApiClient backed by a [MockEngine] that always returns 401. */
    private fun failureClient(): Wave3ApiClient {
        val engine = MockEngine {
            respond(
                content = """{"error":"invalid_credentials"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return Wave3ApiClient(httpClient = client)
    }

    @BeforeTest
    fun setUp() {
        storage = FakeTokenStorage()
        sessionManager = SessionManager(storage)
    }

    @Test
    fun `login success saves session and emits isLoggedIn true`() = runTest {
        val repo = AuthRepository(sessionManager, successClient())
        val result = repo.login("user@example.com", "pass")
        assertTrue(result.isSuccess)
        assertEquals("tok", result.getOrThrow().accessToken)
        assertTrue(repo.isLoggedIn.value)
        assertEquals("tok", sessionManager.getToken()?.accessToken)
    }

    @Test
    fun `login failure does not update session`() = runTest {
        val repo = AuthRepository(sessionManager, failureClient())
        val result = repo.login("user@example.com", "wrong")
        assertTrue(result.isFailure)
        assertFalse(repo.isLoggedIn.value)
        assertNull(sessionManager.getToken())
    }

    @Test
    fun `logout clears session and emits isLoggedIn false`() = runTest {
        val repo = AuthRepository(sessionManager, successClient())
        repo.login("user@example.com", "pass")
        assertTrue(repo.isLoggedIn.value)
        repo.logout()
        assertFalse(repo.isLoggedIn.value)
        assertNull(repo.getCurrentToken())
    }

    @Test
    fun `initialize with valid token keeps session active`() = runTest {
        val validToken = AuthToken(
            accessToken = "valid",
            refreshToken = "refresh",
            expiresIn = 3600,
            expiresAt = Clock.System.now().epochSeconds + 3600
        )
        storage = FakeTokenStorage(initialToken = validToken)
        sessionManager = SessionManager(storage)
        val repo = AuthRepository(sessionManager, successClient())
        repo.initialize()
        assertTrue(repo.isLoggedIn.value)
        assertEquals("valid", repo.getCurrentToken()?.accessToken)
    }

    @Test
    fun `initialize with expired token refreshes automatically`() = runTest {
        val expiredToken = AuthToken(
            accessToken = "old",
            refreshToken = "old-refresh",
            expiresIn = 3600,
            expiresAt = Clock.System.now().epochSeconds - 1  // expired
        )
        storage = FakeTokenStorage(initialToken = expiredToken)
        sessionManager = SessionManager(storage)
        val repo = AuthRepository(sessionManager, successClient())
        repo.initialize()
        // After refresh the new token ("tok") should replace the expired one.
        assertTrue(repo.isLoggedIn.value)
        assertEquals("tok", repo.getCurrentToken()?.accessToken)
    }

    @Test
    fun `initialize with expired token and failed refresh clears session`() = runTest {
        val expiredToken = AuthToken(
            accessToken = "old",
            refreshToken = "old-refresh",
            expiresIn = 3600,
            expiresAt = Clock.System.now().epochSeconds - 1  // expired
        )
        storage = FakeTokenStorage(initialToken = expiredToken)
        sessionManager = SessionManager(storage)
        val repo = AuthRepository(sessionManager, failureClient())
        repo.initialize()
        assertFalse(repo.isLoggedIn.value)
        assertNull(repo.getCurrentToken())
    }

    @Test
    fun `register success saves session and emits isLoggedIn true`() = runTest {
        val repo = AuthRepository(sessionManager, successClient())
        val result = repo.register("user@example.com", "pass", "Test User")
        assertTrue(result.isSuccess)
        assertEquals("tok", result.getOrThrow().accessToken)
        assertTrue(repo.isLoggedIn.value)
        assertNotNull(repo.getCurrentToken())
    }

    @Test
    fun `requestPasswordReset returns success on valid email`() = runTest {
        val repo = AuthRepository(sessionManager, successClient())
        val result = repo.requestPasswordReset("user@example.com")
        assertTrue(result.isSuccess)
    }
}
