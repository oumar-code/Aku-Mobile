package com.akuplatform.android

import android.app.Application
import com.akuplatform.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class AkuApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@AkuApplication)
            // If you use BuildConfig for keys, you can inject them here
        }
    }
}
