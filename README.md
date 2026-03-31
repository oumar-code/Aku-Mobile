# Aku-Mobile

> **Migration Notice:** The Kotlin Multiplatform (KMP) module has been migrated from
> [oumar-code/Akulearn_docs](https://github.com/oumar-code/Akulearn_docs) into this
> dedicated repository. All future mobile development happens here.

Aku-Mobile is the cross-platform mobile codebase for the Akulearn platform, built with
**Kotlin Multiplatform (KMP)** and targeting Android and iOS.

## Getting Started

### Prerequisites

- JDK 17
- Android SDK (set `ANDROID_HOME` or create `local.properties`)
- Xcode 15+ (for iOS targets — macOS only)
- IntelliJ IDEA or Android Studio with the Kotlin Multiplatform plugin

### Clone & Build

```bash
git clone https://github.com/oumar-code/Aku-Mobile.git
cd Aku-Mobile/KOTLIN\ MULTIPLATFORM

# Build all targets
./build-all.sh

# Android only
./gradlew :shared:assembleDebug

# iOS framework (macOS only)
./gradlew :shared:linkDebugFrameworkIosArm64
```

## Repository Structure

```
KOTLIN MULTIPLATFORM/
├── androidApp/          # Android application module
├── shared/              # Shared KMP library (business logic, API, auth)
│   └── src/
│       ├── androidMain/ # Android-specific implementations
│       ├── commonMain/  # Shared Kotlin code (all platforms)
│       └── iosMain/     # iOS-specific implementations
├── gradle/              # Gradle wrapper and version catalog
├── build-all.sh         # Script to build all platform targets
└── settings.gradle.kts  # Project settings
```

## Documentation

- Full module documentation: [`KOTLIN MULTIPLATFORM/README.md`](KOTLIN%20MULTIPLATFORM/README.md)
- Sprint plan: [`KOTLIN MULTIPLATFORM/KMP SPRINT_PLAN.md`](KOTLIN%20MULTIPLATFORM/KMP%20SPRINT_PLAN.md)
- Ecosystem overview: [`docs/ecosystem-map.md`](docs/ecosystem-map.md)
- Mobile docs index: [`docs/03-mobile/index.md`](docs/03-mobile/index.md)
