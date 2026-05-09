import Foundation
import SwiftUI
import shared  // KMP shared framework

@main
struct AkuApp: App {

    // Wire up storage → session → repositories — mirrors the Android Koin graph.
    private let tokenStorage = IosTokenStorage()
    private let sessionManager: SessionManager
    private let authRepository: AuthRepository
    private let courseRepository: CourseRepository
    private let notificationService = IosNotificationService()

    init() {
        let environment = ProcessInfo.processInfo.environment
        let supabaseUrl = environment["SUPABASE_URL"] ?? ""
        let supabaseAnonKey = environment["SUPABASE_ANON_KEY"] ?? ""

        sessionManager = SessionManager(tokenStorage: tokenStorage)

        // Both repositories share the same Supabase credentials.
        // On iOS, each creates its own SupabaseClient since Koin DI is not
        // available at this layer.  Auth uses GoTrue; CourseRepository uses
        // Postgrest and Storage.
        authRepository = AuthRepository(
            sessionManager: sessionManager,
            supabaseUrl: supabaseUrl,
            supabaseAnonKey: supabaseAnonKey
        )
        courseRepository = CourseRepository(
            supabaseUrl: supabaseUrl,
            supabaseAnonKey: supabaseAnonKey,
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
