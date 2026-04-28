plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.akulearn.android"
    compileSdk = 34 // Lowered from 35 to match AGP 8.5.2

    defaultConfig {
        applicationId = "com.akuplatform.android"
        minSdk = 26
        targetSdk = 34 // Lowered from 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "SUPABASE_URL", "\"${System.getenv("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${System.getenv("SUPABASE_ANON_KEY") ?: ""}\"")
        buildConfigField("String", "WAVE3_BASE_URL", "\"${System.getenv("WAVE3_BASE_URL") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release signing: supply these environment variables in CI or locally.
            // Set KEYSTORE_FILE to the path of your .jks / .keystore file.
            // If the variables are absent the build falls back to the debug key (local only).
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            if (keystoreFile != null) {
                signingConfig = signingConfigs.create("release").also { cfg ->
                    cfg.storeFile = file(keystoreFile)
                    cfg.storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                    cfg.keyAlias = System.getenv("KEY_ALIAS") ?: ""
                    cfg.keyPassword = System.getenv("KEY_PASSWORD") ?: ""
                }
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":shared"))

    // Compose BOM and artifacts
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    // Pager (for Onboarding carousel)
    implementation("androidx.compose.foundation:foundation")

    // Android essentials
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Secure token storage and preferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Serialization (required for Json encode/decode in navigation)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Media3 ExoPlayer — in-app video playback for lesson player
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Dependency injection
    implementation("io.insert-koin:koin-android:3.5.6")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
