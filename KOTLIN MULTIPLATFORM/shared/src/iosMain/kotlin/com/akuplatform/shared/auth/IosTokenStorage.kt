package com.akuplatform.shared.auth

import com.akuplatform.shared.auth.model.AuthToken
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * iOS-specific [TokenStorage] backed by [NSUserDefaults].
 *
 * Note: for production apps, consider storing tokens in the iOS Keychain instead
 * of NSUserDefaults to benefit from the OS-level encryption and access controls.
 */
class IosTokenStorage : TokenStorage {

    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun saveToken(token: AuthToken) {
        defaults.setObject(Json.encodeToString(token), forKey = KEY_TOKEN)
    }

    override suspend fun getToken(): AuthToken? {
        val json = defaults.stringForKey(KEY_TOKEN) ?: return null
        return runCatching { Json.decodeFromString<AuthToken>(json) }.getOrNull()
    }

    override suspend fun clearToken() {
        defaults.removeObjectForKey(KEY_TOKEN)
    }

    companion object {
        private const val KEY_TOKEN = "aku_auth_token"
    }
}
