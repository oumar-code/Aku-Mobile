# Contributing to Aku-Mobile

Thank you for helping build the Akulearn mobile platform! This guide covers everything you need to get started.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project layout](#project-layout)
3. [Setting up the dev environment](#setting-up-the-dev-environment)
4. [Building the project](#building-the-project)
5. [Running tests](#running-tests)
6. [Branch naming](#branch-naming)
7. [Pull request conventions](#pull-request-conventions)
8. [Coding standards](#coding-standards)

---

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| JDK | 17 (JDK 17 – 25 all work) |
| Android Studio | Hedgehog (2023.1.1) or later |
| Android SDK | API 26+ (compileSdk 34) |
| Xcode (iOS builds) | 15+ — **macOS only** |

Set `ANDROID_HOME` in your shell profile, or create a `local.properties` file inside `KOTLIN MULTIPLATFORM/` with:

```properties
sdk.dir=/path/to/your/Android/sdk
```

---

## Project layout

```
Aku-Mobile/
├── KOTLIN MULTIPLATFORM/   ← Gradle project root (all Gradle commands run here)
│   ├── shared/             ← KMP library: auth, API, courses, notifications
│   │   └── src/
│   │       ├── commonMain/ ← Shared Kotlin (auth, course, notifications, di)
│   │       ├── androidMain/← Android implementations
│   │       ├── iosMain/    ← iOS/Kotlin-Native implementations
│   │       └── commonTest/ ← Shared unit tests
│   ├── androidApp/         ← Android Jetpack Compose app
│   ├── iosApp/             ← iOS SwiftUI app
│   ├── gradle/             ← Gradle wrapper + version catalog (libs.versions.toml)
│   └── build-all.sh        ← Convenience script to build all targets
├── docs/                   ← Architecture decision records and ecosystem map
└── .github/workflows/      ← CI/CD (build.yml)
```

> **⚠️ Important:** The Gradle project root is `KOTLIN MULTIPLATFORM/`, **not** the repository root.  
> Always `cd "KOTLIN MULTIPLATFORM"` before running `./gradlew` commands, or use the `build-all.sh` script.

---

## Setting up the dev environment

```bash
# Clone the repo
git clone https://github.com/oumar-code/Aku-Mobile.git
cd Aku-Mobile/"KOTLIN MULTIPLATFORM"

# (Optional) create local.properties if ANDROID_HOME is not set
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

Open Android Studio → **File → Open** → select the `KOTLIN MULTIPLATFORM/` directory.

### Environment variables (optional)

| Variable | Purpose |
|----------|---------|
| `WAVE3_BASE_URL` | Override the Wave 3 API base URL (defaults to `https://api.akulearn.com/v3`) |
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_ANON_KEY` | Supabase anonymous key |

These are injected into `BuildConfig` at compile time.

---

## Building the project

```bash
cd "KOTLIN MULTIPLATFORM"

# Build Android debug APK
./gradlew :androidApp:assembleDebug

# Build shared module for Android only
./gradlew :shared:assembleDebug

# Build iOS framework (macOS only)
./gradlew :shared:linkDebugFrameworkIosArm64

# Build all targets
./build-all.sh
```

---

## Running tests

```bash
cd "KOTLIN MULTIPLATFORM"

# Run all shared KMP unit tests (JVM host)
./gradlew :shared:allTests

# Run a specific test class
./gradlew :shared:allTests --tests "com.akuplatform.shared.auth.SessionManagerTest"
```

Tests live in `shared/src/commonTest/`. There are no platform-specific tests yet.

### Test packages

| Package | Tests |
|---------|-------|
| `com.akuplatform.shared.auth` | `AuthRepositoryTest`, `SessionManagerTest` |
| `com.akuplatform.shared.api` | `Wave3ApiClientTest` |
| `com.akuplatform.shared.course` | `CourseRepositoryTest` |
| `com.akuplatform.shared.auth.model` | `AuthTokenTest` |

---

## Branch naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/<short-description>` | `feature/login-ui` |
| Bug fix | `fix/<short-description>` | `fix/token-refresh-crash` |
| Chore / refactor | `chore/<short-description>` | `chore/update-ktor-2.4` |
| Release prep | `release/v<semver>` | `release/v0.2.0` |
| Copilot agent work | `copilot/<description>` | `copilot/current-sprint-phase` |

Branch off from `main`. Do not commit directly to `main`.

---

## Pull request conventions

1. **Title** – Use the imperative mood: *"Add login screen"*, not *"Added login screen"*.
2. **Description** – Explain *what* changed and *why*. Link any related issues.
3. **Checklist** – Ensure CI is green before requesting review.
4. **Squash merge** – We squash all commits when merging to keep the `main` history clean.

### CI checks (enforced on every PR)

| Check | Command |
|-------|---------|
| Build shared (Android) | `./gradlew :shared:assembleDebug` |
| Unit tests | `./gradlew :shared:allTests` |
| Build app (debug APK) | `./gradlew :androidApp:assembleDebug` |
| Build iOS framework | `./gradlew :shared:linkDebugFrameworkIosArm64` *(macOS runner)* |

---

## Coding standards

- **Language:** Kotlin only — no Java.
- **Style:** Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- **Coroutines:** Prefer `suspend` functions over callbacks. Use `StateFlow` for observable state.
- **Error handling:** Return `Result<T>` from API/auth functions; never swallow exceptions silently.
- **No secrets in source code:** API keys and tokens belong in `local.properties` or CI secrets — never in `.kt` files.
