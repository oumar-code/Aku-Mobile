package com.akuplatform.shared.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(
    vararg extraModules: Module,
    appDeclaration: KoinApplication.() -> Unit = {}
): KoinApplication = startKoin {
    appDeclaration()
    if (extraModules.isNotEmpty()) {
        modules(extraModules.toList())
    }
}
