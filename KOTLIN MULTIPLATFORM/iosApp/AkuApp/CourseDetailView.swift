import SwiftUI
import shared  // KMP shared framework

/// Detail screen for a single course.
///
/// Loads lessons and the user's enrolment status on appear, then shows the
/// course description, an enrol button (or "Enrolled" badge), and a lessons
/// section that navigates to `LessonsView`.
struct CourseDetailView: View {

    private let courseRepository: CourseRepository
    let course: Course

    @State private var lessons: [Lesson] = []
    @State private var enrollment: Enrollment? = nil
    @State private var isLoading: Bool = true
    @State private var isEnrolling: Bool = false
    @State private var errorMessage: String? = nil

    init(courseRepository: CourseRepository, course: Course) {
        self.courseRepository = courseRepository
        self.course = course
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {

                // ── Header ────────────────────────────────────────────────────
                VStack(alignment: .leading, spacing: 6) {
                    if !course.instructor.isEmpty {
                        Text("By \(course.instructor)")
                            .font(.subheadline)
                            .foregroundStyle(.tint)
                    }
                    HStack(spacing: 16) {
                        if course.lessonCount > 0 {
                            Label("\(course.lessonCount) lessons", systemImage: "book.closed")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        if course.durationMinutes > 0 {
                            Label("\(course.durationMinutes) min", systemImage: "clock")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                Text(course.description_)
                    .font(.body)

                Divider()

                // ── Enrol button ──────────────────────────────────────────────
                if let error = errorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundStyle(.red)
                }

                if enrollment != nil {
                    NavigationLink {
                        LessonsView(
                            courseRepository: courseRepository,
                            course: course,
                            preloadedLessons: lessons
                        )
                    } label: {
                        Label("Enrolled — View Lessons", systemImage: "checkmark.circle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .tint(.green)
                } else {
                    Button(action: performEnrol) {
                        if isEnrolling {
                            ProgressView()
                                .progressViewStyle(.circular)
                                .frame(maxWidth: .infinity)
                        } else {
                            Text("Enrol in Course")
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(isEnrolling)
                }

                // ── Lessons preview ───────────────────────────────────────────
                if !lessons.isEmpty {
                    Divider()
                    Text("Lessons")
                        .font(.title3.bold())

                    ForEach(
                        lessons.sorted { $0.orderIndex < $1.orderIndex },
                        id: \.id
                    ) { lesson in
                        LessonRowView(lesson: lesson, number: Int(lesson.orderIndex))
                        Divider()
                    }
                }
            }
            .padding(16)
        }
        .navigationTitle(course.title)
        .navigationBarTitleDisplayMode(.inline)
        .overlay {
            if isLoading {
                ProgressView()
                    .progressViewStyle(.circular)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(.ultraThinMaterial)
            }
        }
        .task { await loadDetail() }
    }

    // MARK: - Actions

    private func loadDetail() async {
        isLoading = true
        defer { isLoading = false }
        do {
            // Load lessons and enrolment status in parallel
            async let lessonsResult = courseRepository.getLessons(courseId: course.id)
            async let enrollmentsResult = courseRepository.getEnrolledCourses()

            let lessonsRes = try await lessonsResult
            let enrollRes = try await enrollmentsResult

            lessons = (lessonsRes.getOrNull() as? [Lesson]) ?? []
            let enrollments = (enrollRes.getOrNull() as? [Enrollment]) ?? []
            enrollment = enrollments.first { $0.courseId == course.id }
        } catch {
            // Non-fatal: lessons list will just stay empty
        }
    }

    private func performEnrol() {
        isEnrolling = true
        errorMessage = nil
        Task {
            defer { isEnrolling = false }
            do {
                let result = try await courseRepository.enrollInCourse(courseId: course.id)
                enrollment = result.getOrNull() as? Enrollment
            } catch {
                errorMessage = "Enrolment failed: \(error.localizedDescription)"
            }
        }
    }
}
