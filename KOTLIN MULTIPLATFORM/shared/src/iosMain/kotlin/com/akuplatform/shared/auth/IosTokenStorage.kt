package com.akuplatform.shared.auth

import com.akuplatform.shared.auth.model.AuthToken
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * iOS-specific [TokenStorage] backed by the device Keychain.
 *
 * Tokens are JSON-serialised and stored as a single Keychain entry, benefiting
 * from OS-level encryption and biometric/passcode access controls.
 */
class IosTokenStorage : TokenStorage {

    private val keychain = KeychainHelper(
        service = "com.akulearn.app",
        account = "aku_auth_token"
    )

    override suspend fun saveToken(token: AuthToken) {
        keychain.save(Json.encodeToString(token))
    }

    override suspend fun getToken(): AuthToken? {
        val json = keychain.load() ?: return null
        return runCatching { Json.decodeFromString<AuthToken>(json) }.getOrNull()
    }

    override suspend fun clearToken() {
        keychain.delete()
    }
}
