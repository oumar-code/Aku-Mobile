package com.akuplatform.shared.auth.model

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthTokenTest {

    @Test
    fun `isExpired returns false when expiresAt is 0 (unknown)`() {
        val token = AuthToken("access", "refresh", 3600, expiresAt = 0L)
        assertFalse(token.isExpired())
    }

    @Test
    fun `isExpired returns false when expiresAt is in the future`() {
        val token = AuthToken(
            accessToken = "access",
            refreshToken = "refresh",
            expiresIn = 3600,
            expiresAt = Clock.System.now().epochSeconds + 3600
        )
        assertFalse(token.isExpired())
    }

    @Test
    fun `isExpired returns true when expiresAt is in the past`() {
        val token = AuthToken(
            accessToken = "access",
            refreshToken = "refresh",
            expiresIn = 3600,
            expiresAt = Clock.System.now().epochSeconds - 1
        )
        assertTrue(token.isExpired())
    }
}
