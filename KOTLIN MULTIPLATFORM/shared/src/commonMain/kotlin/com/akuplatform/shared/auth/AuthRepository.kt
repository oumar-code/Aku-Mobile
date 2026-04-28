package com.akuplatform.shared.auth

import com.akuplatform.shared.api.Wave3ApiClient
import com.akuplatform.shared.auth.model.AuthToken
import com.akuplatform.shared.auth.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(
    private val sessionManager: SessionManager,
    private val apiClient: Wave3ApiClient = Wave3ApiClient()
) {

    val isLoggedIn: StateFlow<Boolean> = sessionManager.isLoggedIn

    /**
     * Initialises the session from persisted storage.
     * If a stored token is found but has expired, an automatic token refresh is
     * attempted.  On refresh failure the session is cleared so the user is
     * redirected to login.
     */
    suspend fun initialize() {
        sessionManager.initialize()
        val token = sessionManager.getToken() ?: return
        if (token.isExpired()) {
            refresh(token.refreshToken)
                .onFailure { sessionManager.clearSession() }
        }
    }

    suspend fun login(email: String, password: String): Result<AuthToken> =
        apiClient.authenticate(email, password).onSuccess { token ->
            sessionManager.saveSession(token)
        }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    suspend fun getCurrentToken(): AuthToken? = sessionManager.getToken()

    /** Silently refreshes the access token and persists the new token. */
    suspend fun refresh(refreshToken: String): Result<AuthToken> =
        apiClient.refreshToken(refreshToken).onSuccess { token ->
            sessionManager.saveSession(token)
        }

    suspend fun register(email: String, password: String, name: String): Result<AuthToken> =
        apiClient.register(email, password, name).onSuccess { token ->
            sessionManager.saveSession(token)
        }

    suspend fun requestPasswordReset(email: String): Result<Unit> =
        apiClient.requestPasswordReset(email)

    suspend fun getProfile(): Result<UserProfile> =
        apiClient.getProfile(token = sessionManager.getToken()?.accessToken)

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        apiClient.changePassword(
            currentPassword = currentPassword,
            newPassword = newPassword,
            token = sessionManager.getToken()?.accessToken
        )
}
