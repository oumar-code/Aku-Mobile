package com.akuplatform.shared.auth

import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.auth.model.AuthToken
import com.akuplatform.shared.auth.model.UserProfile
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryTest {

    private val testProfileStreakDays = 4

    private lateinit var storage: FakeTokenStorage
    private lateinit var sessionManager: SessionManager

    @BeforeTest
    fun setUp() {
        storage = FakeTokenStorage()
        sessionManager = SessionManager(storage)
    }

    @Test
    fun `login success saves session and emits isLoggedIn true`() = runTest {
        val token = testToken()
        val service = FakeAuthProviderService(signInToken = token)
        val repo = AuthRepository(sessionManager, service)

        val result = repo.login("user@example.com", "pass")

        assertTrue(result.isSuccess)
        assertEquals(token, result.getOrThrow())
        assertTrue(repo.isLoggedIn.value)
        assertEquals("tok", sessionManager.getToken()?.accessToken)
        assertEquals("user@example.com", service.lastLoginEmail)
    }

    @Test
    fun `login failure does not update session`() = runTest {
        val repo = AuthRepository(
            sessionManager,
            FakeAuthProviderService(signInFailure = ApiError.Unauthorized())
        )

        val result = repo.login("user@example.com", "wrong")

        assertTrue(result.isFailure)
        assertFalse(repo.isLoggedIn.value)
        assertNull(sessionManager.getToken())
    }

    @Test
    fun `logout clears session and emits isLoggedIn false`() = runTest {
        val repo = AuthRepository(sessionManager, FakeAuthProviderService(signInToken = testToken()))
        repo.login("user@example.com", "pass")

        repo.logout()

        assertFalse(repo.isLoggedIn.value)
        assertNull(repo.getCurrentToken())
    }

    @Test
    fun `initialize with valid token imports session and keeps login active`() = runTest {
        val validToken = testToken(expiresAt = 9999999999)
        storage = FakeTokenStorage(initialToken = validToken)
        sessionManager = SessionManager(storage)
        val service = FakeAuthProviderService()
        val repo = AuthRepository(sessionManager, service)

        repo.initialize()

        assertTrue(repo.isLoggedIn.value)
        assertEquals(validToken, repo.getCurrentToken())
        assertEquals(validToken, service.importedToken)
    }

    @Test
    fun `initialize with expired token refreshes automatically`() = runTest {
        val expiredToken = testToken(accessToken = "old", refreshToken = "old-refresh", expiresAt = 1)
        val refreshedToken = testToken(accessToken = "new", refreshToken = "new-refresh")
        storage = FakeTokenStorage(initialToken = expiredToken)
        sessionManager = SessionManager(storage)
        val repo = AuthRepository(
            sessionManager,
            FakeAuthProviderService(refreshTokenResult = refreshedToken)
        )

        repo.initialize()

        assertTrue(repo.isLoggedIn.value)
        assertEquals("new", repo.getCurrentToken()?.accessToken)
    }

    @Test
    fun `initialize with expired token and failed refresh clears session`() = runTest {
        val expiredToken = testToken(accessToken = "old", refreshToken = "old-refresh", expiresAt = 1)
        storage = FakeTokenStorage(initialToken = expiredToken)
        sessionManager = SessionManager(storage)
        val repo = AuthRepository(
            sessionManager,
            FakeAuthProviderService(refreshFailure = ApiError.Unauthorized())
        )

        repo.initialize()

        assertFalse(repo.isLoggedIn.value)
        assertNull(repo.getCurrentToken())
    }

    @Test
    fun `register success saves session and emits isLoggedIn true`() = runTest {
        val token = testToken(accessToken = "registered")
        val repo = AuthRepository(
            sessionManager,
            FakeAuthProviderService(signUpToken = token)
        )

        val result = repo.register("user@example.com", "pass", "Test User")

        assertTrue(result.isSuccess)
        assertEquals("registered", result.getOrThrow().accessToken)
        assertTrue(repo.isLoggedIn.value)
        assertNotNull(repo.getCurrentToken())
    }

    @Test
    fun `requestPasswordReset returns success on valid email`() = runTest {
        val service = FakeAuthProviderService()
        val repo = AuthRepository(sessionManager, service)

        val result = repo.requestPasswordReset("user@example.com")

        assertTrue(result.isSuccess)
        assertEquals("user@example.com", service.lastPasswordResetEmail)
    }

    @Test
    fun `changePassword updates persisted session when provider returns a new token`() = runTest {
        val initialToken = testToken(accessToken = "old")
        val updatedToken = testToken(accessToken = "updated")
        storage = FakeTokenStorage(initialToken = initialToken)
        sessionManager = SessionManager(storage)
        val repo = AuthRepository(
            sessionManager,
            FakeAuthProviderService(changePasswordToken = updatedToken)
        )

        val result = repo.changePassword("old-pass", "new-pass")

        assertTrue(result.isSuccess)
        assertEquals("updated", repo.getCurrentToken()?.accessToken)
    }

    @Test
    fun `getProfile delegates to auth provider`() = runTest {
        val profile = UserProfile(
            id = "user-1",
            name = "Test User",
            email = "user@example.com",
            streakDays = testProfileStreakDays
        )
        val repo = AuthRepository(
            sessionManager,
            FakeAuthProviderService(profile = profile)
        )

        val result = repo.getProfile()

        assertTrue(result.isSuccess)
        assertEquals(profile, result.getOrThrow())
    }

    private fun testToken(
        accessToken: String = "tok",
        refreshToken: String = "ref",
        expiresIn: Long = 3600,
        expiresAt: Long = 9999999999
    ) = AuthToken(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = expiresIn,
        expiresAt = expiresAt
    )
}

private class FakeAuthProviderService(
    private val signInToken: AuthToken = AuthToken("tok", "ref", 3600, 9999999999),
    private val signUpToken: AuthToken = AuthToken("tok", "ref", 3600, 9999999999),
    private val refreshTokenResult: AuthToken = AuthToken("tok", "ref", 3600, 9999999999),
    private val changePasswordToken: AuthToken? = null,
    private val profile: UserProfile = UserProfile(id = "user-1", name = "Test User", email = "user@example.com"),
    private val signInFailure: Throwable? = null,
    private val refreshFailure: Throwable? = null
) : AuthProviderService {

    var importedToken: AuthToken? = null
    var lastLoginEmail: String? = null
    var lastPasswordResetEmail: String? = null

    override suspend fun importSession(token: AuthToken) {
        importedToken = token
    }

    override suspend fun refreshSession(refreshToken: String): AuthToken {
        refreshFailure?.let { throw it }
        return refreshTokenResult
    }

    override suspend fun signIn(email: String, password: String): AuthToken {
        lastLoginEmail = email
        signInFailure?.let { throw it }
        return signInToken
    }

    override suspend fun signUp(email: String, password: String, name: String): AuthToken = signUpToken

    override suspend fun requestPasswordReset(email: String) {
        lastPasswordResetEmail = email
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AuthToken? = changePasswordToken

    override suspend fun getProfile(): UserProfile = profile

    override suspend fun signOut() = Unit
}
