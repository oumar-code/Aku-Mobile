package com.akuplatform.shared.auth.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    /** Duration in seconds until the [accessToken] expires, as returned by the server. */
    val expiresIn: Long,
    /**
     * Unix epoch seconds at which the [accessToken] expires.
     * Computed locally when the token is created; 0 means the expiry is unknown.
     */
    val expiresAt: Long = 0L
) {
    /** Returns true when a known expiry time is in the past. */
    fun isExpired(): Boolean {
        if (expiresAt == 0L) return false
        return Clock.System.now().epochSeconds >= expiresAt
    }
}
