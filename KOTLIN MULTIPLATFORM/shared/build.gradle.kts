plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("app.cash.sqldelight")
}

kotlin {
    // This organizes source sets into a hierarchy (commonMain -> iosMain -> iosX64Main, etc.)
    applyDefaultHierarchyTemplate()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // Define iOS targets. 
    // They are disabled for building on Windows (handled via gradle.properties), 
    // but code in iosMain will still be correctly recognized by the IDE.
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-auth:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-client-logging:2.3.12")
                implementation("io.github.jan-tennert.supabase:gotrue-kt:2.5.4")
                implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.4")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                // Dependency injection
                implementation("io.insert-koin:koin-core:3.5.6")
                // SQLDelight runtime (common)
                implementation("app.cash.sqldelight:runtime:2.0.2")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
                implementation("io.ktor:ktor-client-mock:2.3.12")
                // SQLDelight in-memory SQLite driver for JVM tests
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:2.3.12")
                // SQLDelight Android driver
                implementation("app.cash.sqldelight:android-driver:2.0.2")
            }
        }

        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.12")
                // SQLDelight Native (iOS) driver
                implementation("app.cash.sqldelight:native-driver:2.0.2")
            }
        }
    }
}

sqldelight {
    databases {
        create("AkuDatabase") {
            packageName.set("com.akuplatform.shared.database")
        }
    }
}

android {
    namespace = "com.akuplatform.shared"
    compileSdk = 34 // Lowered from 35 to match AGP 8.5.2
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
