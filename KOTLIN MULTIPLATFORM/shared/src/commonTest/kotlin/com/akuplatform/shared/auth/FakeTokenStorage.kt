package com.akuplatform.shared.auth

import com.akuplatform.shared.auth.model.AuthToken

/** In-memory [TokenStorage] for use in tests. */
class FakeTokenStorage(initialToken: AuthToken? = null) : TokenStorage {

    private var stored: AuthToken? = initialToken

    override suspend fun saveToken(token: AuthToken) {
        stored = token
    }

    override suspend fun getToken(): AuthToken? = stored

    override suspend fun clearToken() {
        stored = null
    }
}
