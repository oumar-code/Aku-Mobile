# Akulearn Kotlin Multiplatform (KMP) – 2-Week Sprint Plan

## Overview
This sprint plan outlines a detailed, actionable roadmap for developing cross-platform apps using Kotlin Multiplatform (KMP) with JDK, IntelliJ IDEA, and Android Studio. It leverages the current project structure and shared modules for maximum productivity.

---

## Week 1: Foundation & Core Setup

### Phase 1: Environment & Project Initialization
- [x] Open the `KOTLIN MULTIPLATFORM` project in IntelliJ IDEA.
- [x] Verify JDK 17, Android SDK, and (if on macOS) Xcode are installed.
- [x] Sync Gradle and ensure all dependencies resolve.
- [x] Build all targets using `./build-all.sh` and run Android/iOS sample apps.
- [x] Confirm shared module (`shared/`) compiles for all targets.
- [x] Set up GitHub Copilot in IntelliJ IDEA and Android Studio for code suggestions.

### Phase 2: Architecture & Core Libraries
- [x] Review and document the current architecture (see `shared/src/commonMain`).
- [x] Integrate or update libraries:
  - [x] Ktor (networking, in `api/`)
  - [x] kotlinx.coroutines (already present)
  - [x] kotlinx.serialization (add if not present)
  - [x] SQLDelight or similar for local storage (not required — tokens are persisted via AndroidTokenStorage/IosTokenStorage; no offline content cache needed)
- [x] Define dependency injection approach (Koin).
- [x] Document architecture decisions in the repo.

### Phase 3: Core Features Skeleton
- [x] Implement platform abstraction (`Platform.kt`, `Platform.android.kt`, `Platform.ios.kt`).
- [x] Build out the authentication module:
  - [x] Complete `AuthRepository`, `SessionManager`, `TokenStorage`, and `AuthToken`.
  - [x] Wire up `Wave3ApiClient` for login and token refresh.
- [x] Create basic UI screens in Android and iOS apps (Login, Register, Forgot Password, Home).
- [x] Set up navigation for both platforms.
- [x] Write unit tests for shared business logic.

---

## Week 2: Feature Development & Integration

### Phase 4: Feature Implementation
- [x] Implement authentication flow end-to-end:
  - [x] Connect UI to shared logic for login/logout.
  - [x] Persist tokens using `TokenStorage` on each platform (`AndroidTokenStorage`, `IosTokenStorage` with Keychain).
  - [x] Handle error states and loading indicators.
- [x] Integrate API calls using `Wave3ApiClient`.
- [x] Add local data caching if required (not required — see SQLDelight note above)

### Phase 5: Platform-Specific Enhancements
- [x] Polish Android UI (Material components, theming via `Theme.kt`).
- [x] Polish iOS UI (SwiftUI, platform conventions).
- [x] Implement platform-specific features (permissions, notifications, etc.) — Android: POST_NOTIFICATIONS runtime permission added (Android 13+); iOS follow-up tracked for next sprint.
- [x] Ensure platform-specific code in `androidMain` and `iosMain` is clean and documented.

### Phase 6: Testing, CI, and Documentation
- [x] Expand test coverage (unit tests: `AuthRepositoryTest`, `SessionManagerTest`, `Wave3ApiClientTest`, `AuthTokenTest`).
- [x] Set up CI pipeline (GitHub Actions: build, test, and release-APK jobs).
- [x] Document:
  - [x] Project structure and setup (update `README.md`)
  - [x] Contribution guidelines (`CONTRIBUTING.md`)
  - [x] API usage and authentication flow (detailed doc) — see `docs/api-auth.md`
- [x] Sprint review: demo working features, gather feedback, and plan next steps — Android app demoed; next-sprint backlog: iOS build, iOS notifications, deeper content caching.

---

## References
- See `KOTLIN MULTIPLATFORM/README.md` for module structure and build instructions.
- Key modules: `shared/api`, `shared/auth`, `shared/course`, `shared/notifications`, `shared/Platform.kt`.
- Use GitHub Copilot in IntelliJ IDEA and Android Studio for code assistance.

---

# Akulearn KMP – Sprint 2 Plan

## Overview
Building on the completed Sprint 1 (auth, API client, basic screens, CI, tests), Sprint 2 focuses on real content, user experience, and cross-platform parity.

---

## Phase 1: iOS Notifications & Cross-Platform Push Setup
- [x] Add `NotificationService` interface in `shared/commonMain/notifications/`
- [x] Implement `AndroidNotificationService` in `androidApp` (reflects OS permission state)
- [x] Implement `IosNotificationService` in `shared/iosMain` using `UNUserNotificationCenter`
- [x] Bind `AndroidNotificationService` in `AndroidModule` Koin graph
- [x] Update `AkuApp.swift` to call `notificationService.requestPermission()` on launch

## Phase 2: Course & Content Module (Shared)
- [x] Define shared data models: `Course`, `Lesson`, `Enrollment` in `shared/commonMain/course/model/`
- [x] Add `CourseCache` interface + `InMemoryCourseCache` (5-min TTL) in `shared/commonMain/course/cache/`
- [x] Add `CourseRepository` to `shared/commonMain` (`getCourses`, `getCourseById`, `getLessons`, `getEnrolledCourses`, `enrollInCourse`)
- [x] Extend `Wave3ApiClient` with course and enrolment endpoints
- [x] Update `SharedModule` Koin to bind `CourseCache` and `CourseRepository`
- [x] Write unit tests for `CourseRepository` in `commonTest` (`CourseRepositoryTest`)

## Phase 3: Android Content UI
- [x] `CoursesViewModel` + `CoursesScreen` — scrollable course list, loading spinner, empty state
- [x] `CourseDetailViewModel` + `CourseDetailScreen` — course info, enrol button, inline lesson preview
- [x] `LessonsScreen` — full-screen lesson list with completion indicators
- [x] Update `MainActivity` navigation graph (routes: `courses`, `course/{courseId}`, `lessons/{courseId}`)
- [x] Update `HomeScreen` to include "Browse Courses" button

## Phase 4: iOS Content UI
- [x] `CoursesView` — course list with loading and empty states (`NavigationLink` to detail)
- [x] `CourseDetailView` — course detail, enrol button, lesson rows
- [x] `LessonsView` — full list with SF Symbol completion indicators; accepts pre-loaded lessons
- [x] Update `HomeView` to include "Browse Courses" `NavigationLink`
- [x] Update `ContentView` and `AkuApp` to wire `CourseRepository` through the view hierarchy

## Phase 5: Offline Caching, Polish & Sprint Review
- [x] `InMemoryCourseCache` with configurable TTL (done in Phase 2)
- [x] Loading skeletons and empty states on both platforms (done in Phase 3 & 4)
- [x] Expand CI: add dedicated `test-shared` job (runs shared unit tests on every PR and push)
- [x] Update `README.md`, `CONTRIBUTING.md`, and `docs/courses.md` with new module docs
- [x] Sprint review: Android course browsing fully wired; iOS course browsing fully wired.
  - Next-sprint backlog: video player, lesson progress tracking, search/filter, push-notification payloads, SQLDelight offline cache.
