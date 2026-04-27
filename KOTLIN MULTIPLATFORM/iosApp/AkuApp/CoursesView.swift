import SwiftUI
import shared  // KMP shared framework

/// Displays the full catalogue of available courses.
///
/// Calls `CourseRepository.getCourses()` on appear and shows a loading spinner,
/// empty state, or a list of tappable `CourseRowView` cards.
struct CoursesView: View {

    private let courseRepository: CourseRepository
    @State private var courses: [Course] = []
    @State private var isLoading: Bool = true
    @State private var errorMessage: String? = nil

    init(courseRepository: CourseRepository) {
        self.courseRepository = courseRepository
    }

    var body: some View {
        Group {
            if isLoading {
                ProgressView()
                    .progressViewStyle(.circular)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = errorMessage {
                VStack(spacing: 16) {
                    Text("Could not load courses")
                        .font(.headline)
                    Text(error)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                    Button("Try Again") { Task { await loadCourses() } }
                        .buttonStyle(.borderedProminent)
                }
                .padding(.horizontal, 32)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if courses.isEmpty {
                VStack(spacing: 8) {
                    Text("No courses available yet.")
                        .font(.body)
                        .foregroundStyle(.secondary)
                    Text("Check back soon!")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(courses, id: \.id) { course in
                    NavigationLink {
                        CourseDetailView(courseRepository: courseRepository, course: course)
                    } label: {
                        CourseRowView(course: course)
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Courses")
        .navigationBarTitleDisplayMode(.large)
        .task { await loadCourses() }
    }

    private func loadCourses() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            // CourseRepository.getCourses() returns Result<List<Course>>.
            // In KMP/Swift, failures propagate as thrown errors.
            let result = try await courseRepository.getCourses()
            courses = (result.getOrNull() as? [Course]) ?? []
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Course row

private struct CourseRowView: View {

    let course: Course

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(course.title)
                .font(.headline)
            if !course.instructor.isEmpty {
                Text("By \(course.instructor)")
                    .font(.caption)
                    .foregroundStyle(.tint)
            }
            Text(course.description_)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(2)
            HStack(spacing: 16) {
                if course.lessonCount > 0 {
                    Label("\(course.lessonCount) lessons", systemImage: "book.closed")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                if course.durationMinutes > 0 {
                    Label("\(course.durationMinutes) min", systemImage: "clock")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}
