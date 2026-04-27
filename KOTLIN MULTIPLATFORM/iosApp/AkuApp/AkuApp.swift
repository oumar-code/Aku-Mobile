import SwiftUI
import shared  // KMP shared framework

@main
struct AkuApp: App {

    // Wire up storage → session → repository — mirrors MainActivity's Koin graph.
    private let tokenStorage = IosTokenStorage()
    private let sessionManager: SessionManager
    private let authRepository: AuthRepository
    private let courseRepository: CourseRepository
    private let notificationService = IosNotificationService()

    init() {
        sessionManager = SessionManager(tokenStorage: tokenStorage)
        authRepository = AuthRepository(
            sessionManager: sessionManager,
            apiClient: Wave3ApiClient()
        )
        courseRepository = CourseRepository(
            apiClient: Wave3ApiClient(),
            sessionManager: sessionManager
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView(
                authRepository: authRepository,
                courseRepository: courseRepository
            )
            .task {
                // Initialize session on launch (reads Keychain; auto-refreshes if expired).
                try? await authRepository.initialize()
                // Request push-notification permission early in the app lifecycle.
                _ = try? await notificationService.requestPermission()
            }
        }
    }
}
