package com.akuplatform.shared.auth

import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.auth.model.AuthToken
import com.akuplatform.shared.auth.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.exception.AuthRestException
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AuthRepository internal constructor(
    private val sessionManager: SessionManager,
    private val authService: AuthProviderService
) {

    constructor(sessionManager: SessionManager, supabaseUrl: String, supabaseAnonKey: String) : this(
        sessionManager = sessionManager,
        authService = createAuthProviderService(supabaseUrl, supabaseAnonKey)
    )

    val isLoggedIn: StateFlow<Boolean> = sessionManager.isLoggedIn

    suspend fun initialize() {
        val token = sessionManager.getToken()
        if (token == null) {
            sessionManager.clearSession()
            return
        }

        runCatching {
            if (token.isExpired()) {
                sessionManager.saveSession(authService.refreshSession(token.refreshToken))
            } else {
                authService.importSession(token)
                sessionManager.initialize()
            }
        }.onFailure {
            sessionManager.clearSession()
        }
    }

    suspend fun login(email: String, pass: String): Result<AuthToken> = runAuthCall {
        authService.signIn(email, pass).also(sessionManager::saveSession)
    }

    suspend fun register(email: String, pass: String, name: String): Result<AuthToken> = runAuthCall {
        authService.signUp(email, pass, name).also(sessionManager::saveSession)
    }

    suspend fun requestPasswordReset(email: String): Result<Unit> = runAuthCall {
        authService.requestPasswordReset(email)
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runAuthCall {
        authService.changePassword(currentPassword, newPassword)?.let(sessionManager::saveSession)
    }

    suspend fun getProfile(): Result<UserProfile> = runAuthCall {
        authService.getProfile()
    }

    suspend fun logout() {
        runCatching { authService.signOut() }
        sessionManager.clearSession()
    }

    suspend fun getCurrentToken(): AuthToken? = sessionManager.getToken()

    private suspend fun <T> runAuthCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (t: Throwable) {
        Result.failure(t.toApiError())
    }
}

internal interface AuthProviderService {
    suspend fun importSession(token: AuthToken)
    suspend fun refreshSession(refreshToken: String): AuthToken
    suspend fun signIn(email: String, password: String): AuthToken
    suspend fun signUp(email: String, password: String, name: String): AuthToken
    suspend fun requestPasswordReset(email: String)
    suspend fun changePassword(currentPassword: String, newPassword: String): AuthToken?
    suspend fun getProfile(): UserProfile
    suspend fun signOut()
}

internal fun createAuthProviderService(
    supabaseUrl: String,
    supabaseAnonKey: String
): AuthProviderService {
    if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
        return MissingAuthProviderService()
    }
    val client = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseAnonKey
    ) {
        install(Auth)
    }
    return SupabaseAuthProviderService(client)
}

internal class MissingAuthProviderService : AuthProviderService {
    private fun fail(): Nothing = throw ApiError.ServerError(
        code = 500,
        message = "Supabase auth is not configured. Set SUPABASE_URL and SUPABASE_ANON_KEY."
    )

    override suspend fun importSession(token: AuthToken) = fail()
    override suspend fun refreshSession(refreshToken: String): AuthToken = fail()
    override suspend fun signIn(email: String, password: String): AuthToken = fail()
    override suspend fun signUp(email: String, password: String, name: String): AuthToken = fail()
    override suspend fun requestPasswordReset(email: String) = fail()
    override suspend fun changePassword(currentPassword: String, newPassword: String): AuthToken = fail()
    override suspend fun getProfile(): UserProfile = fail()
    override suspend fun signOut() = Unit
}

internal class SupabaseAuthProviderService(
    private val client: SupabaseClient
) : AuthProviderService {

    override suspend fun importSession(token: AuthToken) {
        client.auth.importSession(token.toUserSession(), autoRefresh = true)
    }

    override suspend fun refreshSession(refreshToken: String): AuthToken =
        client.auth.refreshSession(refreshToken).toAuthToken()

    override suspend fun signIn(email: String, password: String): AuthToken {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return client.auth.currentSessionOrNull()?.toAuthToken()
            ?: throw ApiError.Unauthorized("No active session was created.")
    }

    override suspend fun signUp(email: String, password: String, name: String): AuthToken {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("name", name)
                put("full_name", name)
            }
        }
        return client.auth.currentSessionOrNull()?.toAuthToken()
            ?: throw ApiError.ServerError(
                code = 409,
                message = "Registration succeeded, but no session was returned. Confirm the account and sign in."
            )
    }

    override suspend fun requestPasswordReset(email: String) {
        client.auth.resetPasswordForEmail(email)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AuthToken? {
        val currentUser = client.auth.currentUserOrNull() ?: client.auth.retrieveUserForCurrentSession(updateSession = true)
        val email = currentUser.email ?: throw ApiError.Unauthorized("No email is available for this account.")

        client.auth.signInWith(Email) {
            this.email = email
            this.password = currentPassword
        }
        client.auth.updateUser {
            password = newPassword
        }
        return client.auth.currentSessionOrNull()?.toAuthToken()
    }

    override suspend fun getProfile(): UserProfile {
        val user = client.auth.currentUserOrNull() ?: client.auth.retrieveUserForCurrentSession(updateSession = true)
        return user.toUserProfile()
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }
}

private fun AuthToken.toUserSession(): UserSession = UserSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresIn = expiresIn,
    tokenType = "bearer",
    expiresAt = Instant.fromEpochSeconds(
        if (expiresAt > 0) expiresAt else Clock.System.now().epochSeconds + expiresIn
    )
)

private fun UserSession.toAuthToken(): AuthToken = AuthToken(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresIn = expiresIn,
    expiresAt = expiresAt.epochSeconds
)

private fun UserInfo.toUserProfile(): UserProfile {
    val metadata = userMetadata
    val appMetadata = appMetadata
    val resolvedName = metadata.string("name")
        ?: metadata.string("full_name")
        ?: appMetadata.string("name")
        ?: appMetadata.string("full_name")
        ?: email?.substringBefore("@").orEmpty()

    return UserProfile(
        id = id,
        name = resolvedName,
        email = email.orEmpty(),
        avatarUrl = metadata.string("avatar_url") ?: appMetadata.string("avatar_url"),
        joinedAt = createdAt?.toString().orEmpty(),
        streakDays = metadata.int("streak_days") ?: appMetadata.int("streak_days") ?: 0
    )
}

private fun JsonObject?.string(key: String): String? =
    this?.get(key)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

private fun JsonObject?.int(key: String): Int? =
    this?.get(key)?.jsonPrimitive?.intOrNull

private fun Throwable.toApiError(): Throwable = when (this) {
    is ApiError -> this
    is AuthRestException -> when (statusCode) {
        400, 401, 403 -> ApiError.Unauthorized(message ?: "Unauthorized.")
        else -> ApiError.ServerError(statusCode, message ?: "Auth request failed.")
    }
    is RestException -> when (statusCode) {
        400, 401, 403 -> ApiError.Unauthorized(message ?: "Unauthorized.")
        else -> ApiError.ServerError(statusCode, message ?: "Supabase request failed.")
    }
    is HttpRequestException -> ApiError.Network(message ?: "Network error.")
    else -> ApiError.Network(message ?: "Unexpected auth error.")
}
