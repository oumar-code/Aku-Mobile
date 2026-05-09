package com.akuplatform.android

import android.app.Application
import com.akulearn.android.di.androidModule
import com.akuplatform.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class AkuApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin(androidModule) {
            androidLogger()
            androidContext(this@AkuApplication)
        }
    }
}
