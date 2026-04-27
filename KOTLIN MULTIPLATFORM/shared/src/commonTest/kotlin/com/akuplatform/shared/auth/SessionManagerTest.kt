package com.akuplatform.shared.auth

import com.akuplatform.shared.auth.model.AuthToken
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionManagerTest {

    private val token = AuthToken("access-abc", "refresh-xyz", 3600)
    private val expiredToken = AuthToken(
        accessToken = "old-access",
        refreshToken = "old-refresh",
        expiresIn = 3600,
        expiresAt = Clock.System.now().epochSeconds - 1  // in the past
    )

    @Test
    fun `isLoggedIn starts false when no token is stored`() = runTest {
        val manager = SessionManager(FakeTokenStorage())
        manager.initialize()
        assertFalse(manager.isLoggedIn.value)
    }

    @Test
    fun `initialize sets isLoggedIn true when a valid token is already persisted`() = runTest {
        val manager = SessionManager(FakeTokenStorage(initialToken = token))
        manager.initialize()
        assertTrue(manager.isLoggedIn.value)
    }

    @Test
    fun `initialize sets isLoggedIn true even when token is expired (expiry handled by AuthRepository)`() = runTest {
        // SessionManager.initialize() only checks presence, not expiry.
        // AuthRepository.initialize() handles expiry and refresh.
        val manager = SessionManager(FakeTokenStorage(initialToken = expiredToken))
        manager.initialize()
        assertTrue(manager.isLoggedIn.value)
    }

    @Test
    fun `saveSession persists token and sets isLoggedIn true`() = runTest {
        val storage = FakeTokenStorage()
        val manager = SessionManager(storage)
        manager.saveSession(token)
        assertTrue(manager.isLoggedIn.value)
        assertNotNull(manager.getToken())
    }

    @Test
    fun `clearSession removes token and sets isLoggedIn false`() = runTest {
        val storage = FakeTokenStorage(initialToken = token)
        val manager = SessionManager(storage)
        manager.initialize()
        manager.clearSession()
        assertFalse(manager.isLoggedIn.value)
        assertNull(manager.getToken())
    }

    @Test
    fun `getToken returns null before any session is saved`() = runTest {
        val manager = SessionManager(FakeTokenStorage())
        assertNull(manager.getToken())
    }
}
