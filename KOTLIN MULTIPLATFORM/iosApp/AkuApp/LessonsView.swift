import SwiftUI
import shared  // KMP shared framework

/// Full-screen lesson list for a course.
///
/// Shows each lesson with its title, duration, and a completion indicator.
/// Accepts an optional `preloadedLessons` array so that `CourseDetailView` can
/// pass the lessons it already fetched, avoiding a duplicate network call.
struct LessonsView: View {

    private let courseRepository: CourseRepository
    let course: Course
    private let preloadedLessons: [Lesson]?

    @State private var lessons: [Lesson] = []
    @State private var isLoading: Bool = false

    init(
        courseRepository: CourseRepository,
        course: Course,
        preloadedLessons: [Lesson]? = nil
    ) {
        self.courseRepository = courseRepository
        self.course = course
        self.preloadedLessons = preloadedLessons
    }

    var body: some View {
        Group {
            if isLoading {
                ProgressView()
                    .progressViewStyle(.circular)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if lessons.isEmpty {
                Text("No lessons available yet.")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(
                    lessons.sorted { $0.orderIndex < $1.orderIndex },
                    id: \.id
                ) { lesson in
                    LessonRowView(lesson: lesson, number: Int(lesson.orderIndex))
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle(course.title)
        .navigationBarTitleDisplayMode(.inline)
        .task { await loadLessons() }
    }

    private func loadLessons() async {
        if let preloaded = preloadedLessons, !preloaded.isEmpty {
            lessons = preloaded
            return
        }
        isLoading = true
        defer { isLoading = false }
        do {
            let result = try await courseRepository.getLessons(courseId: course.id)
            lessons = (result.getOrNull() as? [Lesson]) ?? []
        } catch {
            // Non-fatal: lessons list stays empty
        }
    }
}

// MARK: - Shared lesson row

/// Reusable lesson row used by both `LessonsView` and `CourseDetailView`.
struct LessonRowView: View {

    let lesson: Lesson
    let number: Int

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: lesson.isCompleted ? "checkmark.circle.fill" : "circle")
                .foregroundStyle(lesson.isCompleted ? Color.accentColor : Color.secondary)
                .font(.title3)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(number). \(lesson.title)")
                    .font(.body)
                    .fontWeight(lesson.isCompleted ? .regular : .medium)
                if lesson.durationMinutes > 0 {
                    Text("\(lesson.durationMinutes) min")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
