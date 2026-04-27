package com.akuplatform.shared.course

import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.api.Wave3ApiClient
import com.akuplatform.shared.auth.FakeTokenStorage
import com.akuplatform.shared.auth.SessionManager
import com.akuplatform.shared.auth.model.AuthToken
import com.akuplatform.shared.course.cache.CourseCache
import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.course.model.Enrollment
import com.akuplatform.shared.course.model.Lesson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
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

    private val coursesJson = """
        [
          {"id":"c1","title":"Intro to Python","description":"Learn Python basics","instructor":"Dr. Smith","lesson_count":8,"duration_minutes":240},
          {"id":"c2","title":"Web Dev 101","description":"HTML, CSS, JS","instructor":"Prof. Jones","lesson_count":12,"duration_minutes":360}
        ]
    """.trimIndent()

    private val courseJson = """
        {"id":"c1","title":"Intro to Python","description":"Learn Python basics","instructor":"Dr. Smith","lesson_count":8,"duration_minutes":240}
    """.trimIndent()

    private val lessonsJson = """
        [
          {"id":"l1","course_id":"c1","title":"Variables","duration_minutes":15,"order_index":1,"is_completed":false},
          {"id":"l2","course_id":"c1","title":"Functions","duration_minutes":20,"order_index":2,"is_completed":true}
        ]
    """.trimIndent()

    private val enrollmentsJson = """
        [{"id":"e1","course_id":"c1","user_id":"u1","enrolled_at":"2024-01-01","progress_percent":50}]
    """.trimIndent()

    private val enrollmentJson = """
        {"id":"e2","course_id":"c2","user_id":"u1","enrolled_at":"2024-06-01","progress_percent":0}
    """.trimIndent()

    private fun clientWith(status: HttpStatusCode, body: String): Wave3ApiClient {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return Wave3ApiClient(
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        )
    }

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
    }

    @Test
    fun `getCourses fetches from API and stores in cache`() = runTest {
        val repo = CourseRepository(clientWith(HttpStatusCode.OK, coursesJson), sessionManager, fakeCache)
        val result = repo.getCourses()
        assertTrue(result.isSuccess)
        val courses = result.getOrThrow()
        assertEquals(2, courses.size)
        assertEquals("c1", courses[0].id)
        assertEquals("Intro to Python", courses[0].title)
        assertEquals(1, fakeCache.putCount, "cache should have been populated once")
    }

    @Test
    fun `getCourses returns cached value without hitting the API`() = runTest {
        val seeded = listOf(Course(id = "cached", title = "Cached Course", description = "from cache"))
        fakeCache.seed(seeded)
        // Point the API at an error engine — if it is called the test would fail via 500
        val repo = CourseRepository(clientWith(HttpStatusCode.InternalServerError, "{}"), sessionManager, fakeCache)
        val result = repo.getCourses()
        assertTrue(result.isSuccess)
        assertEquals("cached", result.getOrThrow().first().id)
        assertEquals(0, fakeCache.putCount, "cache should not have been overwritten")
    }

    @Test
    fun `getCourses propagates API errors`() = runTest {
        val repo = CourseRepository(clientWith(HttpStatusCode.InternalServerError, "{}"), sessionManager, fakeCache)
        val result = repo.getCourses()
        assertTrue(result.isFailure)
        assertIs<ApiError.ServerError>(result.exceptionOrNull())
    }

    @Test
    fun `getCourseById returns correct course`() = runTest {
        val repo = CourseRepository(clientWith(HttpStatusCode.OK, courseJson), sessionManager, fakeCache)
        val result = repo.getCourseById("c1")
        assertTrue(result.isSuccess)
        val course = result.getOrThrow()
        assertEquals("c1", course.id)
        assertEquals("Dr. Smith", course.instructor)
    }

    @Test
    fun `getLessons returns lessons in order`() = runTest {
        val repo = CourseRepository(clientWith(HttpStatusCode.OK, lessonsJson), sessionManager, fakeCache)
        val result = repo.getLessons("c1")
        assertTrue(result.isSuccess)
        val lessons = result.getOrThrow()
        assertEquals(2, lessons.size)
        assertEquals("l1", lessons[0].id)
        assertEquals(2, lessons[1].orderIndex)
        assertTrue(lessons[1].isCompleted)
    }

    @Test
    fun `getEnrolledCourses returns enrollments`() = runTest {
        val repo = CourseRepository(clientWith(HttpStatusCode.OK, enrollmentsJson), sessionManager, fakeCache)
        val result = repo.getEnrolledCourses()
        assertTrue(result.isSuccess)
        val enrollments = result.getOrThrow()
        assertEquals(1, enrollments.size)
        assertEquals(50, enrollments[0].progressPercent)
    }

    @Test
    fun `enrollInCourse returns new enrollment and invalidates cache`() = runTest {
        val seeded = listOf(Course(id = "c1", title = "T", description = "D"))
        fakeCache.seed(seeded)
        val repo = CourseRepository(clientWith(HttpStatusCode.OK, enrollmentJson), sessionManager, fakeCache)
        val result = repo.enrollInCourse("c2")
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        // Cache should be invalidated so next getCourses call goes to API
        assertEquals(null, fakeCache.getCourses(), "cache should be cleared after enrollment")
    }

    @Test
    fun `enrollInCourse propagates API error without touching cache`() = runTest {
        val seeded = listOf(Course(id = "c1", title = "T", description = "D"))
        fakeCache.seed(seeded)
        val repo = CourseRepository(clientWith(HttpStatusCode.Unauthorized, "{}"), sessionManager, fakeCache)
        val result = repo.enrollInCourse("c2")
        assertTrue(result.isFailure)
        assertIs<ApiError.Unauthorized>(result.exceptionOrNull())
        // Cache should still be intact
        assertNotNull(fakeCache.getCourses(), "cache should not be cleared on failed enrollment")
    }
}
