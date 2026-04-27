# Course & Content Module

This document describes the `course` package added in Sprint 2 of the Akulearn KMP project.

## Overview

The course module gives learners access to the full Akulearn content catalogue.  It is implemented as a shared KMP module (`shared/commonMain`) so the same business logic is used on both Android and iOS.

---

## Data Models (`com.akuplatform.shared.course.model`)

### `Course`
| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Unique course identifier |
| `title` | `String` | Display name |
| `description` | `String` | Short course overview |
| `imageUrl` | `String?` | Optional cover image URL |
| `instructor` | `String` | Instructor name |
| `lessonCount` | `Int` | Total number of lessons |
| `durationMinutes` | `Int` | Estimated completion time |

### `Lesson`
| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Unique lesson identifier |
| `courseId` | `String` | Parent course reference |
| `title` | `String` | Lesson title |
| `description` | `String` | Short description |
| `durationMinutes` | `Int` | Lesson length |
| `orderIndex` | `Int` | Lesson position in the course (1-based) |
| `isCompleted` | `Boolean` | Whether the current user has completed this lesson |

### `Enrollment`
| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Unique enrolment identifier |
| `courseId` | `String` | Enrolled course |
| `userId` | `String` | User who enrolled |
| `enrolledAt` | `String` | ISO-8601 enrolment timestamp |
| `progressPercent` | `Int` | Overall course completion (0–100) |

---

## CourseRepository (`com.akuplatform.shared.course`)

All methods return `Result<T>` and are `suspend` functions.

| Method | Description |
|--------|-------------|
| `getCourses()` | Catalogue of all available courses (cache-first, 5-min TTL) |
| `getCourseById(id)` | Fetch a single course by ID (always network) |
| `getLessons(courseId)` | Ordered list of lessons for a course (always network) |
| `getEnrolledCourses()` | Current user's enrolment records |
| `enrollInCourse(courseId)` | Enrol the current user; invalidates the courses cache |
| `invalidateCache()` | Force the next `getCourses()` call to fetch fresh data |

### Example (Android ViewModel)

```kotlin
viewModelScope.launch {
    courseRepository.getCourses()
        .onSuccess { courses -> /* update UI */ }
        .onFailure { error -> /* show error */ }
}
```

---

## CourseCache (`com.akuplatform.shared.course.cache`)

`InMemoryCourseCache` is the default implementation.  It keeps a single list of `Course` objects in memory with a configurable TTL (default: **5 minutes**).

To change the TTL, provide a custom `CourseCache` binding in the Koin module:

```kotlin
single<CourseCache> { InMemoryCourseCache(ttlMs = 10 * 60 * 1000L) } // 10 minutes
```

---

## API Endpoints (Wave 3)

| Method | Path | Auth required |
|--------|------|---------------|
| `GET` | `/courses` | ✅ |
| `GET` | `/courses/{id}` | ✅ |
| `GET` | `/courses/{id}/lessons` | ✅ |
| `GET` | `/enrollments` | ✅ |
| `POST` | `/enrollments` | ✅ |

All authenticated endpoints expect a `Bearer <access_token>` header.  The token is automatically injected by `CourseRepository` via `SessionManager.getToken()`.

---

## Android UI

Screens are in `androidApp/src/main/kotlin/.../ui/`:

- **`CoursesScreen`** – `LazyColumn` of course cards; loading spinner and empty state.
- **`CourseDetailScreen`** – Course info, enrol button, and inline lesson list.
- **`LessonsScreen`** – Full-screen lesson list with completion indicators.

Navigation routes added to `MainActivity`:
- `courses` → `CoursesScreen`
- `course/{courseId}` → `CourseDetailScreen`
- `lessons/{courseId}` → `LessonsScreen`

---

## iOS UI

Views are in `iosApp/AkuApp/`:

- **`CoursesView`** – `List` of courses; loading and empty states.
- **`CourseDetailView`** – Course info, enrol button, and lesson rows.
- **`LessonsView`** – `List` of lessons with SF Symbol completion indicators.

Navigation uses `NavigationStack` + `NavigationLink` from `HomeView`.
