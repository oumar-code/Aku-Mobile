package com.akulearn.android.di

import com.akulearn.android.BuildConfig
import com.akulearn.android.auth.AndroidTokenStorage
import com.akulearn.android.notifications.AndroidNotificationService
import com.akuplatform.shared.api.Wave3ApiClient
import com.akuplatform.shared.auth.TokenStorage
import com.akuplatform.shared.di.sharedModule
import com.akuplatform.shared.notifications.NotificationService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val androidModule: Module = module {
    includes(sharedModule)
    single<TokenStorage> { AndroidTokenStorage(androidContext()) }
    single<NotificationService> { AndroidNotificationService(androidContext()) }
    // Use the environment-specific base URL injected at build time; fall back to the
    // hard-coded production URL when the env var is absent (local/debug builds).
    single(override = true) {
        Wave3ApiClient(baseUrl = BuildConfig.WAVE3_BASE_URL.ifBlank { Wave3ApiClient.BASE_URL })
    }
}
