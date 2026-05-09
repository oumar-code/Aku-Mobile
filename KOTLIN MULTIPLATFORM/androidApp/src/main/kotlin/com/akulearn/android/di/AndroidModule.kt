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
    includes(
        sharedModule(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY
        )
    )
    single<TokenStorage> { AndroidTokenStorage(androidContext()) }
    single<NotificationService> { AndroidNotificationService(androidContext()) }
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
    single<LessonProgressStorage> { AndroidLessonProgressStorage(androidContext()) }
}
