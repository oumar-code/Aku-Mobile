package com.akuplatform.shared.di

import com.akuplatform.shared.api.Wave3ApiClient
import com.akuplatform.shared.auth.AuthRepository
import com.akuplatform.shared.auth.SessionManager
import com.akuplatform.shared.auth.TokenStorage
import com.akuplatform.shared.course.CourseRepository
import com.akuplatform.shared.course.cache.CourseCache
import com.akuplatform.shared.course.cache.InMemoryCourseCache
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for shared (platform-agnostic) dependencies.
 *
 * Platform modules must provide a [TokenStorage] binding before including this module.
 *
 * @param baseUrl Optional API base URL override. Defaults to [Wave3ApiClient.BASE_URL].
 *                Pass a blank string to use the default as well.
 */
fun sharedModule(baseUrl: String = Wave3ApiClient.BASE_URL): Module = module {
    single { SessionManager(get()) }
    single { Wave3ApiClient(baseUrl = baseUrl.ifBlank { Wave3ApiClient.BASE_URL }) }
    single { AuthRepository(get(), get()) }
    single<CourseCache> { InMemoryCourseCache() }
    single { CourseRepository(get(), get(), get()) }
}
