# Aku-Mobile — App Progress

**Repository:** [oumar-code/Aku-Mobile](https://github.com/oumar-code/Aku-Mobile)  
**Platform:** Kotlin Multiplatform (KMP) — Android & iOS  
**Last updated:** 2026-04-28

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Repository Structure](#repository-structure)
4. [Sprint 1 — Foundation & Core](#sprint-1--foundation--core)
5. [Sprint 2 — Course & Content](#sprint-2--course--content)
6. [Sprint 3 — UX Polish & Platform Features](#sprint-3--ux-polish--platform-features)
7. [Current Feature Status](#current-feature-status)
8. [CI/CD Pipeline](#cicd-pipeline)
9. [Test Coverage](#test-coverage)
10. [Next-Sprint Backlog](#next-sprint-backlog)

---

## Project Overview

Aku-Mobile is the cross-platform mobile application for the **Akulearn** learning platform. It is
built with **Kotlin Multiplatform (KMP)**, sharing business logic (auth, API, courses,
notifications) across Android and iOS while each platform hosts its own native UI layer.

The KMP module was migrated from the `oumar-code/Akulearn_docs` repository into this dedicated
repository to keep all mobile development in one place.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Shared business logic | Kotlin Multiplatform (KMP) |
| Android UI | Jetpack Compose + Material 3 |
| iOS UI | SwiftUI |
| Networking | Ktor (multiplatform HTTP client) |
| Serialisation | kotlinx.serialization |
| Async | kotlinx.coroutines + `StateFlow` |
| Dependency injection | Koin (`sharedModule` + `androidModule`) |
| Token storage — Android | `EncryptedSharedPreferences` (AES-256-GCM, Android Keystore) |
| Token storage — iOS | System Keychain (`IosTokenStorage`) |
| Video playback — Android | ExoPlayer (Media3) |
| Notifications — Android | `AndroidNotificationService` (POST_NOTIFICATIONS, Android 13+) |
| Notifications — iOS | `IosNotificationService` (`UNUserNotificationCenter`) |
| CI/CD | GitHub Actions |
| Build system | Gradle (Kotlin DSL) + version catalog (`libs.versions.toml`) |

---

## Repository Structure

```
Aku-Mobile/
├── KOTLIN MULTIPLATFORM/       ← Gradle project root (all Gradle commands run here)
│   ├── shared/                 ← KMP library: auth, API, courses, notifications, DI
│   │   └── src/
│   │       ├── commonMain/     ← Shared Kotlin (auth, course, notifications, di)
│   │       ├── androidMain/    ← Android-specific implementations
│   │       ├── iosMain/        ← iOS-specific implementations
│   │       └── commonTest/     ← Shared unit tests
│   ├── androidApp/             ← Android Jetpack Compose app
│   ├── iosApp/                 ← iOS SwiftUI app
│   ├── gradle/                 ← Gradle wrapper + version catalog
│   └── build-all.sh            ← Convenience script to build all targets
├── docs/                       ← Architecture decisions and ecosystem map
├── .github/workflows/          ← CI/CD (build.yml)
├── CONTRIBUTING.md
├── README.md
├── automation_progress.md      ← Automated migration checklist
└── APP_PROGRESS.md             ← This file
```

---

## Sprint 1 — Foundation & Core

> **Status: ✅ Complete**

### Phase 1 — Environment & Project Initialisation
- [x] Repository created and KMP module migrated from `Akulearn_docs`
- [x] Initial project structure seeded (`androidApp`, `shared`, Gradle wrapper)
- [x] JDK 17 / Android SDK / Xcode compatibility confirmed
- [x] Gradle sync and all-targets build verified

### Phase 2 — Architecture & Core Libraries
- [x] Architecture reviewed and documented
- [x] Ktor integrated for networking (`api/`)
- [x] kotlinx.coroutines and kotlinx.serialization integrated
- [x] Koin selected and configured for dependency injection
- [x] Architecture decisions documented

### Phase 3 — Core Features Skeleton
- [x] Platform abstraction (`Platform.kt`, `Platform.android.kt`, `Platform.ios.kt`)
- [x] Authentication module:
  - `AuthRepository` — login, logout, register
  - `SessionManager` — `StateFlow`-backed session state
  - `TokenStorage` interface + `AndroidTokenStorage` / `IosTokenStorage`
  - `AuthToken` data class with expiry tracking
- [x] `Wave3ApiClient` — login, register, token refresh, password reset
- [x] Basic UI screens on Android and iOS: Login, Register, Forgot Password, Home
- [x] Navigation set up on both platforms
- [x] Unit tests for shared business logic

### Phase 4 — Authentication Flow (end-to-end)
- [x] UI connected to shared auth logic for login/logout
- [x] Tokens persisted using `EncryptedSharedPreferences` (Android) and Keychain (iOS)
- [x] Error states and loading indicators wired to UI

### Phase 5 — CI, Testing & Documentation
- [x] GitHub Actions CI pipeline (build-android, build-ios jobs)
- [x] Root `README.md` with migration notice and getting-started guide
- [x] `CONTRIBUTING.md` with branch naming, PR conventions, and coding standards
- [x] `docs/api-auth.md` — detailed auth flow and API endpoint reference
- [x] `docs/ecosystem-map.md` — Aku-Mobile status updated to **Active**
- [x] `docs/03-mobile/index.md` — Mobile section linked to new repo

---

## Sprint 2 — Course & Content

> **Status: ✅ Complete**

### Phase 1 — iOS Notifications & Cross-Platform Push Setup
- [x] `NotificationService` interface in `shared/commonMain/notifications/`
- [x] `AndroidNotificationService` — reflects OS permission state (POST_NOTIFICATIONS, Android 13+)
- [x] `IosNotificationService` — triggers `UNUserNotificationCenter` permission dialog
- [x] `AndroidNotificationService` bound in `AndroidModule` Koin graph
- [x] `AkuApp.swift` calls `notificationService.requestPermission()` on launch

### Phase 2 — Course & Content Module (Shared)
- [x] Data models: `Course`, `Lesson`, `Enrollment`, `Certificate` in `shared/commonMain/course/model/`
- [x] `CourseCache` interface + `InMemoryCourseCache` (configurable TTL, default 5 min)
- [x] `CourseRepository` — `getCourses`, `getCourseById`, `getLessons`, `getEnrolledCourses`, `enrollInCourse`, `invalidateCache`
- [x] `Wave3ApiClient` extended with course and enrolment endpoints
- [x] Koin `SharedModule` updated to bind `CourseCache` and `CourseRepository`
- [x] `CourseRepositoryTest` added to `commonTest`

### Phase 3 — Android Content UI
- [x] `CoursesViewModel` + `CoursesScreen` — scrollable course list, loading spinner, empty state
- [x] `CourseDetailViewModel` + `CourseDetailScreen` — course info, enrol button, lesson preview
- [x] `LessonsScreen` — full-screen lesson list with completion indicators
- [x] Navigation routes: `courses`, `course/{courseId}`, `lessons/{courseId}`
- [x] `HomeScreen` updated with "Browse Courses" button

### Phase 4 — iOS Content UI
- [x] `CoursesView` — course list with loading and empty states, `NavigationLink` to detail
- [x] `CourseDetailView` — course detail, enrol button, lesson rows
- [x] `LessonsView` — full list with SF Symbol completion indicators
- [x] `HomeView` updated with "Browse Courses" `NavigationLink`
- [x] `ContentView` and `AkuApp` wired `CourseRepository` through the view hierarchy

### Phase 5 — CI Expansion & Documentation
- [x] Dedicated `test-shared` CI job (runs shared unit tests on every PR and push)
- [x] `docs/courses.md` — full course module API reference and UI documentation
- [x] `README.md` and `CONTRIBUTING.md` updated with course module info

---

## Sprint 3 — UX Polish & Platform Features

> **Status: ✅ Complete**

### Navigation & Shell
- [x] Bottom navigation bar (Material 3 `NavigationBar`) with five tabs:
  - **Home** (`home`)
  - **Courses** (`courses`)
  - **My Learning** (`certificates`)
  - **Profile** (`profile`)
  - **Settings** (`settings`)

### New Screens — Android
- [x] **Onboarding** (`OnboardingScreen`) — animated horizontal pager with feature highlights
- [x] **Splash** — initial loading screen with branded logo
- [x] **Profile** (`ProfileScreen`) — user details, avatar initial, navigation to Settings
- [x] **Settings** (`SettingsScreen`) — notification toggle, privacy policy link, logout action
- [x] **Certificates** (`CertificatesScreen`) — list of earned certificates with share action
- [x] **Privacy Policy** (`PrivacyPolicyScreen`) — static policy text
- [x] **Rate Us Dialog** (`RateUsDialog`) — in-app rating prompt
- [x] **Lesson Player** (`LessonPlayerScreen`) — ExoPlayer video playback with quiz UI integration
  - Fullscreen landscape support
  - Playback controls (play/pause, seek)
  - Inline quiz card below the player

### Video Playback
- [x] ExoPlayer (Media3) integrated in `LessonPlayerScreen`
- [x] `LessonPlayerViewModel` manages playback state and lesson progress

### Shared Data Model Updates
- [x] `Certificate` data model added (`id`, `courseId`, `courseTitle`, `userId`, `userName`, `issuedAt`)
- [x] `UserProfile` model added to `auth/model/`

### Bug Fixes
- [x] Logout flow fixed — `clearSession()` now correctly navigates back to LoginScreen
- [x] Android build configuration corrected (dependency alignment, build error resolved)

### Branding & Theme
- [x] `Theme.kt` refined — consistent Material 3 colour scheme across all screens
- [x] App icon and branding assets updated

---

## Current Feature Status

### Authentication

| Feature | Android | iOS |
|---|---|---|
| Login screen | ✅ | ✅ |
| Register screen | ✅ | ✅ |
| Forgot password screen | ✅ | ✅ |
| Token persistence (encrypted) | ✅ | ✅ |
| Auto-login on app restart | ✅ | ✅ |
| Token refresh | ✅ | ✅ |
| Logout | ✅ | ✅ |

### Course & Content

| Feature | Android | iOS |
|---|---|---|
| Course catalogue list | ✅ | ✅ |
| Course detail page | ✅ | ✅ |
| Lesson list | ✅ | ✅ |
| Enrolment | ✅ | ✅ |
| Lesson video player | ✅ | ⏳ (next sprint) |
| In-lesson quiz UI | ✅ | ⏳ (next sprint) |
| In-memory course cache (5-min TTL) | ✅ | ✅ |

### User Profile & Settings

| Feature | Android | iOS |
|---|---|---|
| Profile screen | ✅ | ⏳ |
| Settings screen | ✅ | ⏳ |
| Certificates screen | ✅ | ⏳ |
| Privacy policy screen | ✅ | ⏳ |
| In-app rating prompt | ✅ | ⏳ |

### Onboarding

| Feature | Android | iOS |
|---|---|---|
| Splash screen | ✅ | ⏳ |
| Onboarding pager | ✅ | ⏳ |

### Notifications

| Feature | Android | iOS |
|---|---|---|
| Push permission request | ✅ | ✅ |
| Notification service abstraction | ✅ | ✅ |

---

## CI/CD Pipeline

The GitHub Actions workflow (`.github/workflows/build.yml`) runs on every push to `main` and
every pull request.

```
[test-shared]
    │  Shared unit tests (ubuntu-latest)
    │  ./gradlew :shared:allTests
    ▼
[build-android]
    │  Shared module + Android debug APK (ubuntu-latest)
    │  ./gradlew :shared:assembleDebug
    │  ./gradlew :androidApp:assembleDebug
    │  Uploads debug APK as a build artifact (14-day retention)
    │
    ├─► [build-ios]   (continue-on-error; macos-latest)
    │       ./gradlew :shared:compileKotlinIosArm64
    │
    └─► [release-apk]  (only on `v*` tags)
            ./gradlew :androidApp:assembleRelease
            Publishes release APK to GitHub Releases
```

---

## Test Coverage

Tests live in `shared/src/commonTest/` and run on the JVM host via `./gradlew :shared:allTests`.

| Test Class | Module | What it covers |
|---|---|---|
| `AuthRepositoryTest` | `auth` | Login, logout, register flows |
| `SessionManagerTest` | `auth` | `StateFlow` session state, token restore |
| `Wave3ApiClientTest` | `api` | HTTP request construction, error mapping |
| `AuthTokenTest` | `auth/model` | Token expiry logic |
| `CourseRepositoryTest` | `course` | Catalogue fetch, enrolment, cache invalidation |

---

## Next-Sprint Backlog

- [ ] iOS lesson video player (AVPlayer integration)
- [ ] iOS quiz UI (inline quiz card after video)
- [ ] iOS profile, settings, and certificates screens
- [ ] Lesson progress tracking (persist completed lessons per user)
- [ ] SQLDelight offline content cache (replace in-memory cache with persistent storage)
- [ ] Push notification payloads (deep-link into specific lessons)
- [ ] Course search and filter
- [ ] Lesson progress bar and resume-from-last-position
- [ ] Release first versioned APK (`v0.1.0` tag → release job)
