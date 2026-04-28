package com.akuplatform.shared.di

import com.akuplatform.shared.api.Wave3ApiClient
import com.akuplatform.shared.auth.AuthRepository
import com.akuplatform.shared.auth.SessionManager
import com.akuplatform.shared.auth.TokenStorage
import com.akuplatform.shared.course.CourseRepository
import com.akuplatform.shared.course.cache.CourseCache
import com.akuplatform.shared.course.cache.SqlDelightCourseCache
import com.akuplatform.shared.course.progress.LessonProgressStorage
import com.akuplatform.shared.database.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for shared (platform-agnostic) dependencies.
 *
 * Platform modules must provide the following bindings before including this module:
 * - [TokenStorage]          — secure token persistence (required)
 * - [DatabaseDriverFactory] — SQLite driver factory (required)
 * - [LessonProgressStorage] — lesson completion persistence (optional)
 *
 * @param baseUrl Optional API base URL override. Defaults to [Wave3ApiClient.BASE_URL].
 *                Pass a blank string to use the default as well.
 */
fun sharedModule(baseUrl: String = Wave3ApiClient.BASE_URL): Module = module {
    single { SessionManager(get()) }
    single { Wave3ApiClient(baseUrl = baseUrl.ifBlank { Wave3ApiClient.BASE_URL }) }
    single { AuthRepository(get(), get()) }
    single<CourseCache> { SqlDelightCourseCache(get<DatabaseDriverFactory>()) }
    single {
        CourseRepository(
            apiClient = get(),
            sessionManager = get(),
            cache = get(),
            progressStorage = getOrNull<LessonProgressStorage>()
        )
    }
}
