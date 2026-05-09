package com.akuplatform.shared.di

import com.akuplatform.shared.auth.AuthRepository
import com.akuplatform.shared.auth.SessionManager
import com.akuplatform.shared.auth.TokenStorage
import com.akuplatform.shared.course.CourseRepository
import com.akuplatform.shared.course.cache.CourseCache
import com.akuplatform.shared.course.cache.SqlDelightCourseCache
import com.akuplatform.shared.course.progress.LessonProgressStorage
import com.akuplatform.shared.database.DatabaseDriverFactory
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for shared (platform-agnostic) dependencies.
 *
 * A single [SupabaseClient] is created as a singleton and shared by both
 * [AuthRepository] (uses the [Auth] plugin) and [CourseRepository] (uses the
 * [Postgrest] and [Storage] plugins).  This matches the recommended pattern for
 * supabase-kt in a KMP application.
 *
 * Platform modules must provide the following bindings before including this module:
 * - [TokenStorage]          — secure token persistence (required)
 * - [DatabaseDriverFactory] — SQLite driver factory (required)
 * - [LessonProgressStorage] — lesson completion persistence (optional)
 *
 * @param supabaseUrl      Supabase project URL.  Required for a functional app.
 *                         If blank, the client will be created with an empty URL and
 *                         all network operations will fail with a connection error;
 *                         no configuration-time exception is thrown.
 * @param supabaseAnonKey  Supabase anonymous API key.  Same caveat as [supabaseUrl].
 */
fun sharedModule(
    supabaseUrl: String = "",
    supabaseAnonKey: String = ""
): Module = module {
    // ── Supabase client (shared singleton) ────────────────────────────────────
    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseAnonKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }

    // ── Auth ──────────────────────────────────────────────────────────────────
    single { SessionManager(get<TokenStorage>()) }
    single { AuthRepository(get<SessionManager>(), get<SupabaseClient>()) }

    // ── Course ────────────────────────────────────────────────────────────────
    single<CourseCache> { SqlDelightCourseCache(get<DatabaseDriverFactory>()) }
    single {
        CourseRepository(
            supabaseClient = get(),
            sessionManager = get(),
            cache = get(),
            progressStorage = getOrNull<LessonProgressStorage>()
        )
    }
}

