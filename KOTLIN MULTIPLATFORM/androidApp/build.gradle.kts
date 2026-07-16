import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

// Load local.properties so developers can supply credentials without setting
// environment variables.  Environment variables take precedence (for CI).
val localProps = Properties().also { props ->
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) props.load(localFile.inputStream())
}

fun credential(name: String): String =
    System.getenv(name) ?: localProps.getProperty(name) ?: ""

val supabaseUrl = Regex("^https://supabase\\.com/dashboard/project/([a-z0-9-]+)$")
    .matchEntire(credential("SUPABASE_URL"))
    ?.groupValues
    ?.getOrNull(1)
    ?.let { "https://$it.supabase.co" }
    ?: credential("SUPABASE_URL")
val supabaseAnonKey = credential("SUPABASE_ANON_KEY")

android {
    namespace = "com.akulearn.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.akuplatform.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
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
            // If the variables are absent the build falls back to the debug key so
            // GitHub release APKs remain installable.
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            if (keystoreFile != null) {
                val storePassword = requireNotNull(System.getenv("KEYSTORE_PASSWORD")) {
                    "KEYSTORE_FILE is set but KEYSTORE_PASSWORD is missing"
                }
                val keyAlias = requireNotNull(System.getenv("KEY_ALIAS")) {
                    "KEYSTORE_FILE is set but KEY_ALIAS is missing"
                }
                val keyPassword = requireNotNull(System.getenv("KEY_PASSWORD")) {
                    "KEYSTORE_FILE is set but KEY_PASSWORD is missing"
                }
                signingConfig = signingConfigs.create("release").also { cfg ->
                    cfg.storeFile = file(keystoreFile)
                    cfg.storePassword = storePassword
                    cfg.keyAlias = keyAlias
                    cfg.keyPassword = keyPassword
                }
            } else {
                signingConfig = signingConfigs.getByName("debug")
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
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    // Pager (for Onboarding carousel)
    implementation(libs.compose.foundation)

    // Android essentials
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Secure token storage and preferences
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    // Serialization (required for Json encode/decode in navigation)
    implementation(libs.kotlinx.serialization.json)

    // Media3 ExoPlayer — in-app video playback for lesson player
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Dependency injection
    implementation(libs.koin.android)

    debugImplementation(libs.compose.ui.tooling)

    // Firebase BOM — pin all Firebase library versions consistently
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
}
