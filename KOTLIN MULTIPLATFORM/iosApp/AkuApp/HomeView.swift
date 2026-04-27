import SwiftUI
import shared  // KMP shared framework

struct HomeView: View {

    private let authRepository: AuthRepository
    private let courseRepository: CourseRepository
    @ObservedObject private var authState: AuthState
    @State private var isLoggingOut: Bool = false

    init(
        authRepository: AuthRepository,
        courseRepository: CourseRepository,
        authState: AuthState
    ) {
        self.authRepository = authRepository
        self.courseRepository = courseRepository
        self.authState = authState
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Spacer()

                Text("Welcome to Akulearn!")
                    .font(.headline)
                    .multilineTextAlignment(.center)

                Spacer().frame(height: 48)

                NavigationLink {
                    CoursesView(courseRepository: courseRepository)
                } label: {
                    Text("Browse Courses")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .padding(.horizontal, 32)

                Spacer().frame(height: 16)

                Button(action: performLogout) {
                    if isLoggingOut {
                        ProgressView()
                            .progressViewStyle(.circular)
                            .frame(maxWidth: .infinity)
                    } else {
                        Text("Log Out")
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(.red)
                .disabled(isLoggingOut)
                .padding(.horizontal, 32)

                Spacer()
            }
            .navigationTitle("Akulearn")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func performLogout() {
        isLoggingOut = true
        Task {
            defer { isLoggingOut = false }
            await authRepository.logout()
        }
    }
}
