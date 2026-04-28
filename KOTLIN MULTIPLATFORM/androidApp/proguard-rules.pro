# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep shared KMP data models (used by kotlinx.serialization)
-keep class com.akuplatform.shared.** { *; }

# Keep Ktor and OkHttp classes used by networking
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Keep kotlinx.serialization runtime
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.akuplatform.**$$serializer { *; }
-keepclassmembers class com.akuplatform.** {
    *** Companion;
}
-keepclasseswithmembers class com.akuplatform.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Koin
-keep class org.koin.** { *; }

# WebView JS interface
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

