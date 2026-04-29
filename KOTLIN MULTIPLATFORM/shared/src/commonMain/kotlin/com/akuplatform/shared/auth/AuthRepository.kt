package com.akuplatform.shared.auth

import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan_tennert.supabase.gotrue.auth
import io.github.jan_tennert.supabase.gotrue.providers.builtin.Email

interface AuthRepository {
    suspend fun login(email: String, pass: String): Result<Unit>
}

class AuthRepositoryImpl(private val supabase: SupabaseClient) : AuthRepository {
    override suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
