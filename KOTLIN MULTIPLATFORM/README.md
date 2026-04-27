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
│       │       ├── api/               # API clients (Wave3ApiClient)
│       │       ├── auth/              # Authentication (AuthRepository, SessionManager, TokenStorage)
│       │       ├── course/            # Course content (CourseRepository, models, CourseCache)
│       │       │   ├── model/         # Course, Lesson, Enrollment data classes
│       │       │   └── cache/         # CourseCache interface + InMemoryCourseCache
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
- **Wave3ApiClient** – HTTP client for the Akulearn Wave 3 REST API. Covers auth and course endpoints.

### `com.akuplatform.shared.auth`
- **AuthRepository** – High-level authentication operations (login, logout, register).
- **SessionManager** – Manages the active user session using `StateFlow`.
- **TokenStorage** – Interface for persisting `AuthToken` on each platform.
- **model/AuthToken** – Data class holding access token, refresh token, and expiry.

### `com.akuplatform.shared.course`
- **CourseRepository** – Course catalogue, lesson loading, and enrolment management.
- **cache/CourseCache** – Interface + `InMemoryCourseCache` with configurable TTL (5 min default).
- **model/Course** – Course metadata (title, instructor, lesson count, duration).
- **model/Lesson** – Individual lesson with ordering and completion state.
- **model/Enrollment** – User enrolment record with progress percentage.

### `com.akuplatform.shared.notifications`
- **NotificationService** – Cross-platform interface for push-notification permission management.
  - Android: `AndroidNotificationService` (reflects OS grant state)
  - iOS: `IosNotificationService` (triggers `UNUserNotificationCenter` permission dialog)

## Notes

- `local.properties` and `.gradle/` are excluded from version control via `.gitignore`.
- The `gradlew` / `gradlew.bat` wrapper scripts are committed so builds work without a local Gradle installation.
- Dependency injection is handled by Koin (`sharedModule` for platform-agnostic bindings; `androidModule` for Android-specific bindings).
