package com.akulearn.android.di

import com.akulearn.android.BuildConfig
import com.akulearn.android.auth.AndroidLessonProgressStorage
import com.akulearn.android.auth.AndroidTokenStorage
import com.akulearn.android.notifications.AndroidNotificationService
import com.akuplatform.shared.auth.TokenStorage
import com.akuplatform.shared.course.progress.LessonProgressStorage
import com.akuplatform.shared.database.AndroidDatabaseDriverFactory
import com.akuplatform.shared.database.DatabaseDriverFactory
import com.akuplatform.shared.di.sharedModule
import com.akuplatform.shared.notifications.NotificationService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val androidModule: Module = module {
    // Pass the build-time base URL to the shared module; sharedModule falls back
    // to the hard-coded production URL when the env var is absent (local/debug builds).
    includes(sharedModule(BuildConfig.WAVE3_BASE_URL))
    single<TokenStorage> { AndroidTokenStorage(androidContext()) }
    single<NotificationService> { AndroidNotificationService(androidContext()) }
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
    single<LessonProgressStorage> { AndroidLessonProgressStorage(androidContext()) }
}
