# Course & Content Module

This document describes the `course` package of the Akulearn KMP project.

## Overview

The course module gives learners access to the full Akulearn content catalogue.  It is implemented as a shared KMP module (`shared/commonMain`) so the same business logic is used on both Android and iOS.

All data access goes through **Supabase**:
- Relational data (courses, lessons, enrollments, certificates) → **Supabase Postgrest**
- Time-limited media content links (videos, PDFs) → **Supabase Storage signed URLs**
- Row-Level-Security (RLS) policies on the Supabase project ensure that each
  authenticated user can only read / write their own records automatically.

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
| `category` | `String` | Topic category |

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
| `contentUrl` | `String` | Storage path or external URL for media |
| `contentTypeRaw` | `String` | `"video"`, `"text"`, or `"quiz"` |

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
| `getCourses()` | Catalogue of all available courses (SQLDelight cache-first, 5-min TTL) |
| `getCourseById(id)` | Fetch a single course by ID (always Supabase) |
| `getLessons(courseId)` | Ordered list of lessons for a course (always Supabase) |
| `getSignedContentUrl(lesson)` | Time-limited signed URL for the lesson's media content |
| `getEnrolledCourses()` | Current user's enrolment records |
| `enrollInCourse(courseId)` | Enrol the current user; invalidates the courses cache |
| `markLessonComplete(lessonId)` | Records completion in Supabase + local EncryptedSharedPreferences |
| `getCompletedLessons()` | Returns locally-cached completed lesson IDs |
| `searchCourses(query)` | Client-side title/instructor filter (cache-first) |
| `filterCourses(category)` | Client-side category filter (cache-first) |
| `getCertificates()` | Returns the user's earned certificates |
| `invalidateCache()` | Force the next `getCourses()` call to fetch fresh data |

### Signed URL TTLs

| Content type | TTL |
|---|---|
| `VIDEO` | 4 hours (14 400 s) |
| `TEXT`, `QUIZ`, other | 1 hour (3 600 s) |

> **Note:** Signed URLs expire — do not cache them beyond a single playback session.

### Example (Android ViewModel)

```kotlin
viewModelScope.launch {
    courseRepository.getCourses()
        .onSuccess { courses -> /* update UI */ }
        .onFailure { error -> /* show error */ }
}

// Before starting video playback:
val signedUrl = courseRepository.getSignedContentUrl(lesson).getOrNull() ?: lesson.contentUrl
```

---

## CourseCache (`com.akuplatform.shared.course.cache`)

`SqlDelightCourseCache` is the default implementation, backed by the local SQLite
database via SQLDelight 2.0.2.  It keeps a single list of `Course` objects with a
configurable TTL (default: **5 minutes**).

To change the TTL, provide a custom `CourseCache` binding in the Koin module:

```kotlin
single<CourseCache> { SqlDelightCourseCache(get(), ttlMs = 10 * 60 * 1000L) } // 10 min
```

---

## Supabase Tables

| Table | Purpose |
|---|---|
| `courses` | Full course catalogue |
| `lessons` | Lessons within each course, ordered by `order_index` |
| `enrollments` | User–course enrolments (RLS-scoped to current user) |
| `lesson_completions` | Per-user lesson completion records (RLS-scoped) |
| `certificates` | Earned certificates (RLS-scoped to current user) |

Media files are stored in the **`course-content`** Supabase Storage bucket.

---

## Android UI

Screens are in `androidApp/src/main/kotlin/.../ui/`:

- **`CoursesScreen`** – `LazyColumn` of course cards; loading spinner and empty state.
- **`CourseDetailScreen`** – Course info, enrol button, and inline lesson list.
- **`LessonsScreen`** – Full-screen lesson list with per-lesson progress bars.
- **`LessonPlayerScreen`** – ExoPlayer video playback with auto-completion at ≥ 90% and resume position.

Navigation routes:
- `courses` → `CoursesScreen`
- `course/{courseId}` → `CourseDetailScreen`
- `lessons/{courseId}` → `LessonsScreen`
- `lesson/{lessonId}` → `LessonPlayerScreen`

---

## iOS UI

Views are in `iosApp/AkuApp/`:

- **`CoursesView`** – `List` of courses; loading and empty states.
- **`CourseDetailView`** – Course info, enrol button, and lesson rows.
- **`LessonsView`** – `List` of lessons with SF Symbol completion indicators.

Navigation uses `NavigationStack` + `NavigationLink` from `HomeView`.

