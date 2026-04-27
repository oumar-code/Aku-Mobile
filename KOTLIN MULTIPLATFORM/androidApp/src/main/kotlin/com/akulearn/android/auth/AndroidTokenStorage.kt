package com.akulearn.android.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.akuplatform.shared.auth.TokenStorage
import com.akuplatform.shared.auth.model.AuthToken
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aku_auth")

/**
 * Android implementation of [TokenStorage] that persists tokens using [DataStore].
 *
 * Note: DataStore does not encrypt data on disk by default. For production, consider
 * layering [androidx.security.crypto.EncryptedFile] or using the Jetpack Security library
 * to encrypt the DataStore backing file.
 */
class AndroidTokenStorage(private val context: Context) : TokenStorage {

    override suspend fun saveToken(token: AuthToken) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = token.accessToken
            prefs[KEY_REFRESH_TOKEN] = token.refreshToken
            prefs[KEY_EXPIRES_IN] = token.expiresIn
        }
    }

    override suspend fun getToken(): AuthToken? {
        val prefs = context.dataStore.data.first()
        val accessToken = prefs[KEY_ACCESS_TOKEN] ?: return null
        val refreshToken = prefs[KEY_REFRESH_TOKEN] ?: return null
        val expiresIn = prefs[KEY_EXPIRES_IN] ?: return null
        return AuthToken(accessToken, refreshToken, expiresIn)
    }

    override suspend fun clearToken() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_EXPIRES_IN = longPreferencesKey("expires_in")
    }
}
