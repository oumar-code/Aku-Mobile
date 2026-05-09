pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application",
                "com.android.library" -> useModule("com.android.tools.build:gradle:8.5.2")
                "org.jetbrains.kotlin.android",
                "org.jetbrains.kotlin.multiplatform",
                "org.jetbrains.kotlin.plugin.compose",
                "org.jetbrains.kotlin.plugin.serialization" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
                "app.cash.sqldelight" -> useModule("app.cash.sqldelight:gradle-plugin:2.0.2")
                "com.google.gms.google-services" -> useModule("com.google.gms:google-services:4.4.2")
            }
        }
    }
    plugins {
        id("com.android.application") version "8.5.2"
        id("com.android.library") version "8.5.2"
        id("org.jetbrains.kotlin.android") version "2.1.0"
        id("org.jetbrains.kotlin.multiplatform") version "2.1.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
        id("app.cash.sqldelight") version "2.0.2"
        id("com.google.gms.google-services") version "4.4.2"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AkulearnKMP"
include(":androidApp")
include(":shared")
