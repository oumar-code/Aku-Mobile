package com.akuplatform.shared.auth

import com.akuplatform.shared.api.Wave3ApiClient
import com.akuplatform.shared.auth.model.AuthToken
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(
    private val sessionManager: SessionManager,
    private val apiClient: Wave3ApiClient = Wave3ApiClient()
) {

    val isLoggedIn: StateFlow<Boolean> = sessionManager.isLoggedIn

    suspend fun login(email: String, password: String): Result<AuthToken> =
        apiClient.authenticate(email, password).onSuccess { token ->
            sessionManager.saveSession(token)
        }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    suspend fun getCurrentToken(): AuthToken? = sessionManager.getToken()
}
