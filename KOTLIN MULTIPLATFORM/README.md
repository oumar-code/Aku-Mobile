# Akulearn Kotlin Multiplatform (KMP) Module

This directory contains the shared Kotlin Multiplatform code for the Akulearn platform, targeting Android and iOS.

## Structure

```
KOTLIN MULTIPLATFORM/
├── androidApp/          # Android application module
├── shared/              # Shared KMP library (business logic, API, auth, courses)
│   └── src/
│       ├── androidMain/ # Android-specific implementations
│       ├── commonMain/  # Shared Kotlin code (all platforms)
│       │   └── kotlin/com/akuplatform/shared/
│       │       ├── api/               # API clients (Wave3ApiClient - deprecated)
│       │       ├── auth/              # Authentication (AuthRepository, SessionManager, TokenStorage)
│       │       ├── course/            # Course content (CourseRepository, SupabaseCourseDataSource, models)
│       │       │   ├── model/         # Course, Lesson, Enrollment data classes
│       │       │   ├── cache/         # CourseCache interface + SqlDelightCourseCache
│       │       │   └── progress/      # LessonProgressStorage interface
│       │       ├── notifications/     # NotificationService interface
│       │       └── di/                # Koin dependency injection modules
│       ├── iosMain/     # iOS-specific implementations (IosTokenStorage, IosNotificationService)
│       └── commonTest/  # Shared unit tests (auth, course)
├── iosApp/              # iOS SwiftUI application
├── gradle/              # Gradle wrapper and version catalog
├── build-all.sh         # Script to build all platform targets
└── settings.gradle.kts  # Project settings
```

## Prerequisites

- JDK 17 or later (JDK 25 works — source/target compatibility is Java 17)
- Android SDK (set `ANDROID_HOME` or create `local.properties` in this directory)
- Xcode (for iOS targets, macOS only)

## Local Shell Environment

Set Supabase variables in your current shell session before running Gradle:

```bash
export SUPABASE_URL="https://supabase.com/dashboard/project/rcxiuzzwwwschjcbeenb"
export SUPABASE_ANON_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJjeGl1enp3d3dzY2hqY2JlZW5iIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAzNzA1NTAsImV4cCI6MjA4NTk0NjU1MH0.QdrCEnJ0xM9eXlhSpj1fLS4th83e4TJTYb-3QTrGbC8"
```

To persist across terminal restarts:

```bash
cat <<'EOF' >> ~/.bashrc
export SUPABASE_URL="https://supabase.com/dashboard/project/rcxiuzzwwwschjcbeenb"
export SUPABASE_ANON_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJjeGl1enp3d3dzY2hqY2JlZW5iIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAzNzA1NTAsImV4cCI6MjA4NTk0NjU1MH0.QdrCEnJ0xM9eXlhSpj1fLS4th83e4TJTYb-3QTrGbC8"
EOF
source ~/.bashrc
```

The Android Gradle config normalizes this dashboard URL to the API URL automatically.

## Building

```bash
# Build all targets
./build-all.sh

# Build Android only
./gradlew :shared:assembleDebug

# Run shared unit tests
./gradlew :shared:allTests

# Build iOS framework (macOS only)
./gradlew :shared:linkDebugFrameworkIosArm64
```

## Key Modules

### `com.akuplatform.shared.api`
- **Wave3ApiClient** – _(Deprecated)_ Legacy HTTP client for the Wave 3 REST API. No longer used in production; replaced by `SupabaseClient`.

### `com.akuplatform.shared.auth`
- **AuthRepository** – High-level Supabase authentication operations (login, logout, register, password reset, profile, password change).
- **SessionManager** – Manages the active user session using `StateFlow`.
- **TokenStorage** – Interface for persisting `AuthToken` on each platform.
- **model/AuthToken** – Data class holding access token, refresh token, and expiry.

### `com.akuplatform.shared.course`
- **CourseRepository** – Course catalogue, lesson loading, enrolment, and Supabase Storage signed URL generation for media content.
- **SupabaseCourseDataSource** – Supabase Postgrest + Storage backend for all course data operations.
- **cache/SqlDelightCourseCache** – SQLDelight-backed cache with 5-minute TTL for offline course access.
- **model/Course** – Course metadata (title, instructor, lesson count, duration, category).
- **model/Lesson** – Individual lesson with ordering, completion state, and content URL.
- **model/Enrollment** – User enrolment record with progress percentage.
- **progress/LessonProgressStorage** – Interface for local lesson completion persistence.

### `com.akuplatform.shared.notifications`
- **NotificationService** – Cross-platform interface for push-notification permission management.
  - Android: `AndroidNotificationService` (reflects OS grant state)
  - iOS: `IosNotificationService` (triggers `UNUserNotificationCenter` permission dialog)

## Notes

- `local.properties` and `.gradle/` are excluded from version control via `.gitignore`.
- The `gradlew` / `gradlew.bat` wrapper scripts are committed so builds work without a local Gradle installation.
- Dependency injection is handled by Koin (`sharedModule` for platform-agnostic bindings; `androidModule` for Android-specific bindings).
- All Supabase operations require `SUPABASE_URL` and `SUPABASE_ANON_KEY` environment variables (both auth and course data go through the same `SupabaseClient` singleton).

