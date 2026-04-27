package com.akulearn.android.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.akuplatform.shared.auth.TokenStorage
import com.akuplatform.shared.auth.model.AuthToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [TokenStorage] backed by [EncryptedSharedPreferences].
 *
 * All values are encrypted on disk using AES256-SIV (keys) and AES256-GCM (values)
 * via the Jetpack Security library.  The master key is stored in the Android Keystore.
 */
class AndroidTokenStorage(context: Context) : TokenStorage {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun saveToken(token: AuthToken) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token.accessToken)
            .putString(KEY_REFRESH_TOKEN, token.refreshToken)
            .putLong(KEY_EXPIRES_IN, token.expiresIn)
            .putLong(KEY_EXPIRES_AT, token.expiresAt)
            .apply()
    }

    override suspend fun getToken(): AuthToken? = withContext(Dispatchers.IO) {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return@withContext null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return@withContext null
        val expiresIn = if (prefs.contains(KEY_EXPIRES_IN)) prefs.getLong(KEY_EXPIRES_IN, 0L)
        else return@withContext null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        AuthToken(accessToken, refreshToken, expiresIn, expiresAt)
    }

    override suspend fun clearToken() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "aku_auth_secure"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
