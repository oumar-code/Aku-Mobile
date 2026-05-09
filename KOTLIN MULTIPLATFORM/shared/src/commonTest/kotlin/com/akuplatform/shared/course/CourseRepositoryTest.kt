package com.akuplatform.shared.course

import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.auth.FakeTokenStorage
import com.akuplatform.shared.auth.SessionManager
import com.akuplatform.shared.auth.model.AuthToken
import com.akuplatform.shared.course.cache.CourseCache
import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.course.model.Enrollment
import com.akuplatform.shared.course.model.Lesson
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** In-memory [CourseCache] double that records every [putCourses] invocation for assertions. */
private class FakeCourseCache : CourseCache {
    private var cached: List<Course>? = null
    var putCount = 0

    override suspend fun getCourses(): List<Course>? = cached

    override suspend fun putCourses(courses: List<Course>) {
        cached = courses
        putCount++
    }

    override suspend fun invalidate() {
        cached = null
    }

    fun seed(courses: List<Course>) { cached = courses }
}

class CourseRepositoryTest {

    private lateinit var storage: FakeTokenStorage
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeCache: FakeCourseCache
    private lateinit var fakeProgress: FakeLessonProgressStorage

    private val catalogueCourses = listOf(
        Course(id = "c1", title = "Intro to Python", description = "Learn Python basics",
            instructor = "Dr. Smith", lessonCount = 8, durationMinutes = 240, category = "Programming"),
        Course(id = "c2", title = "Web Dev 101", description = "HTML, CSS, JS",
            instructor = "Prof. Jones", lessonCount = 12, durationMinutes = 360, category = "Web")
    )

    private val courseLessons = listOf(
        Lesson(id = "l1", courseId = "c1", title = "Variables", durationMinutes = 15,
            orderIndex = 1, isCompleted = false),
        Lesson(id = "l2", courseId = "c1", title = "Functions", durationMinutes = 20,
            orderIndex = 2, isCompleted = true)
    )

    private val courseEnrollments = listOf(
        Enrollment(id = "e1", courseId = "c1", userId = "u1",
            enrolledAt = "2024-01-01", progressPercent = 50)
    )

    private fun repoWith(
        dataSource: FakeCourseDataSource,
        cache: FakeCourseCache = fakeCache,
        progress: FakeLessonProgressStorage? = null
    ) = CourseRepository(
        sessionManager = sessionManager,
        dataSource = dataSource,
        cache = cache,
        progressStorage = progress
    )

    @BeforeTest
    fun setUp() {
        val validToken = AuthToken(
            accessToken = "test-token",
            refreshToken = "refresh",
            expiresIn = 3600,
            expiresAt = Clock.System.now().epochSeconds + 3600
        )
        storage = FakeTokenStorage(initialToken = validToken)
        sessionManager = SessionManager(storage)
        fakeCache = FakeCourseCache()
        fakeProgress = FakeLessonProgressStorage()
    }

    // ── getCourses ─────────────────────────────────────────────────────────────

    @Test
    fun `getCourses fetches from data source and stores in cache`() = runTest {
        val repo = repoWith(FakeCourseDataSource(courses = catalogueCourses))
        val result = repo.getCourses()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertEquals("c1", result.getOrThrow()[0].id)
        assertEquals(1, fakeCache.putCount, "cache should have been populated once")
    }

    @Test
    fun `getCourses returns cached value without hitting the data source`() = runTest {
        val seeded = listOf(Course(id = "cached", title = "Cached Course", description = "from cache"))
        fakeCache.seed(seeded)
        // Data source always throws — if it is called the test fails via exception
        val repo = repoWith(FakeCourseDataSource(shouldThrow = ApiError.ServerError(500)))
        val result = repo.getCourses()
        assertTrue(result.isSuccess)
        assertEquals("cached", result.getOrThrow().first().id)
        assertEquals(0, fakeCache.putCount, "cache should not have been overwritten")
    }

    @Test
    fun `getCourses propagates data source errors`() = runTest {
        val repo = repoWith(FakeCourseDataSource(shouldThrow = ApiError.ServerError(500)))
        val result = repo.getCourses()
        assertTrue(result.isFailure)
        assertIs<ApiError.ServerError>(result.exceptionOrNull())
    }

    // ── getCourseById ──────────────────────────────────────────────────────────

    @Test
    fun `getCourseById returns correct course`() = runTest {
        val repo = repoWith(FakeCourseDataSource(courses = catalogueCourses))
        val result = repo.getCourseById("c1")
        assertTrue(result.isSuccess)
        assertEquals("c1", result.getOrThrow().id)
        assertEquals("Dr. Smith", result.getOrThrow().instructor)
    }

    // ── getLessons ─────────────────────────────────────────────────────────────

    @Test
    fun `getLessons returns lessons for course`() = runTest {
        val repo = repoWith(FakeCourseDataSource(lessonsMap = mapOf("c1" to courseLessons)))
        val result = repo.getLessons("c1")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertEquals("l1", result.getOrThrow()[0].id)
        assertEquals(2, result.getOrThrow()[1].orderIndex)
        assertTrue(result.getOrThrow()[1].isCompleted)
    }

    // ── getEnrolledCourses ────────────────────────────────────────────────────

    @Test
    fun `getEnrolledCourses returns enrollments`() = runTest {
        val repo = repoWith(FakeCourseDataSource(enrollments = courseEnrollments))
        val result = repo.getEnrolledCourses()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals(50, result.getOrThrow()[0].progressPercent)
    }

    // ── enrollInCourse ────────────────────────────────────────────────────────

    @Test
    fun `enrollInCourse returns new enrollment and invalidates cache`() = runTest {
        val seeded = listOf(Course(id = "c1", title = "T", description = "D"))
        fakeCache.seed(seeded)
        val repo = repoWith(FakeCourseDataSource(courses = catalogueCourses))
        val result = repo.enrollInCourse("c2")
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals(null, fakeCache.getCourses(), "cache should be cleared after enrollment")
    }

    @Test
    fun `enrollInCourse propagates error without touching cache`() = runTest {
        val seeded = listOf(Course(id = "c1", title = "T", description = "D"))
        fakeCache.seed(seeded)
        val repo = repoWith(FakeCourseDataSource(shouldThrow = ApiError.Unauthorized()))
        val result = repo.enrollInCourse("c2")
        assertTrue(result.isFailure)
        assertIs<ApiError.Unauthorized>(result.exceptionOrNull())
        assertNotNull(fakeCache.getCourses(), "cache should not be cleared on failed enrollment")
    }

    // ── searchCourses ─────────────────────────────────────────────────────────

    @Test
    fun `searchCourses returns all cached courses for blank query`() = runTest {
        val seeded = listOf(
            Course(id = "c1", title = "Python", description = "", instructor = "Smith"),
            Course(id = "c2", title = "Kotlin", description = "", instructor = "Jones")
        )
        fakeCache.seed(seeded)
        val repo = repoWith(FakeCourseDataSource(shouldThrow = ApiError.ServerError(500)))
        assertEquals(2, repo.searchCourses("").getOrThrow().size)
    }

    @Test
    fun `searchCourses filters by title (case-insensitive)`() = runTest {
        val seeded = listOf(
            Course(id = "c1", title = "Intro to Python", description = "", instructor = "Smith"),
            Course(id = "c2", title = "Advanced Kotlin", description = "", instructor = "Jones")
        )
        fakeCache.seed(seeded)
        val repo = repoWith(FakeCourseDataSource())
        val result = repo.searchCourses("python")
        assertEquals(1, result.getOrThrow().size)
        assertEquals("c1", result.getOrThrow().first().id)
    }

    @Test
    fun `searchCourses filters by instructor`() = runTest {
        val seeded = listOf(
            Course(id = "c1", title = "Python", description = "", instructor = "Dr Smith"),
            Course(id = "c2", title = "Kotlin", description = "", instructor = "Prof Jones")
        )
        fakeCache.seed(seeded)
        val repo = repoWith(FakeCourseDataSource())
        val result = repo.searchCourses("smith")
        assertEquals(1, result.getOrThrow().size)
        assertEquals("c1", result.getOrThrow().first().id)
    }

    // ── filterCourses ─────────────────────────────────────────────────────────

    @Test
    fun `filterCourses returns all when category is blank`() = runTest {
        val seeded = listOf(
            Course(id = "c1", title = "Python", description = "", category = "Programming"),
            Course(id = "c2", title = "Design", description = "", category = "Art")
        )
        fakeCache.seed(seeded)
        val repo = repoWith(FakeCourseDataSource())
        assertEquals(2, repo.filterCourses("").getOrThrow().size)
    }

    @Test
    fun `filterCourses narrows to exact category (case-insensitive)`() = runTest {
        val seeded = listOf(
            Course(id = "c1", title = "Python", description = "", category = "Programming"),
            Course(id = "c2", title = "Design", description = "", category = "Art"),
            Course(id = "c3", title = "Kotlin", description = "", category = "programming")
        )
        fakeCache.seed(seeded)
        val repo = repoWith(FakeCourseDataSource())
        val ids = repo.filterCourses("Programming").getOrThrow().map { it.id }
        assertEquals(2, ids.size)
        assertTrue(ids.containsAll(listOf("c1", "c3")))
    }

    // ── Lesson progress ───────────────────────────────────────────────────────

    @Test
    fun `markLessonComplete persists lesson id to progressStorage`() = runTest {
        val repo = repoWith(FakeCourseDataSource(), progress = fakeProgress)
        val result = repo.markLessonComplete("lesson-99")
        assertTrue(result.isSuccess)
        assertTrue(repo.getCompletedLessons().contains("lesson-99"))
    }

    @Test
    fun `markLessonComplete does not persist on data source failure`() = runTest {
        val repo = repoWith(
            FakeCourseDataSource(shouldThrow = ApiError.ServerError(500)),
            progress = fakeProgress
        )
        val result = repo.markLessonComplete("lesson-99")
        assertTrue(result.isFailure)
        assertTrue(repo.getCompletedLessons().isEmpty())
    }

    @Test
    fun `getCompletedLessons returns empty set when no progressStorage`() = runTest {
        val repo = repoWith(FakeCourseDataSource())
        assertTrue(repo.getCompletedLessons().isEmpty())
    }
}

