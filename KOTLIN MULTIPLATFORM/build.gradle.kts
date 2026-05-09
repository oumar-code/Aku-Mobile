// Declare all plugins at root with apply false so Gradle loads their classloaders
// in the root scope – shared across all subprojects. This is required by Gradle 8.x
// to prevent KotlinNativeBundleBuildService type mismatches when the Kotlin plugin is
// applied independently in sibling projects (:shared and :androidApp).
plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("org.jetbrains.kotlin.multiplatform") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("app.cash.sqldelight") apply false
    id("com.google.gms.google-services") apply false
}
