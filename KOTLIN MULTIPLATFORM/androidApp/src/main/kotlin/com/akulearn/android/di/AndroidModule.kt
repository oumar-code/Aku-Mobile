package com.akulearn.android.di

import com.akulearn.android.auth.AndroidTokenStorage
import com.akulearn.android.notifications.AndroidNotificationService
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
}
